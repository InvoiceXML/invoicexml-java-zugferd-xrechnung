package com.invoicexml.zugferdxrechnung;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.transport.TransportResponse;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.time.Clock;
import java.util.List;

/** Maps an error response to the matching exception type. */
final class ErrorMapper {

    private ErrorMapper() {
    }

    static InvoiceXmlApiException toException(TransportResponse response, ObjectMapper mapper, Clock clock) {
        String body = response.bodyAsString();
        JsonNode node = parse(body, mapper);
        Integer errorCode = intOrNull(node, "errorCode");
        String title = textOrNull(node, "title");
        String detail = textOrNull(node, "detail");
        String traceId = textOrNull(node, "traceId");
        int status = response.statusCode();

        switch (status) {
            case 401:
                return new AuthenticationException(errorCode, title, detail, traceId, body);
            case 413:
                return new PayloadTooLargeException(errorCode, title, detail, traceId, body);
            case 429:
                return new RateLimitException(errorCode, title, detail, traceId, body,
                        RetryAfter.parse(response.header("Retry-After"), clock));
            default:
                break;
        }
        if (status == 400 && node != null && node.path("errors").isArray()) {
            return new InvoiceValidationException(status, errorCode, title, detail, traceId, body,
                    findings(node.get("errors"), mapper),
                    findings(node.get("warnings"), mapper),
                    invoice(node.get("invoiceData"), mapper));
        }
        return new InvoiceXmlApiException(status, errorCode, title, detail, traceId, body);
    }

    private static JsonNode parse(String body, ObjectMapper mapper) {
        if (body.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(body);
            return node != null && node.isObject() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<ValidationFinding> findings(JsonNode array, ObjectMapper mapper) {
        if (array == null || !array.isArray()) {
            return null;
        }
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, ValidationFinding.class);
        try {
            return mapper.convertValue(array, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static InvoiceDocument invoice(JsonNode node, ObjectMapper mapper) {
        if (node == null || !node.isObject()) {
            return null;
        }
        try {
            return mapper.convertValue(node, InvoiceDocument.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.canConvertToInt()) {
            return v.asInt();
        }
        try {
            return Integer.valueOf(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
