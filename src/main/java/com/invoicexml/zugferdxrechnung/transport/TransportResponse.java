package com.invoicexml.zugferdxrechnung.transport;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A raw HTTP response: status code, headers and body bytes.
 */
public final class TransportResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    /**
     * Creates a response.
     *
     * @param statusCode the HTTP status code
     * @param headers the response headers; names are matched case-insensitively
     * @param body the response body, empty for none
     */
    public TransportResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        Map<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            copy.putAll(headers);
        }
        this.headers = Collections.unmodifiableMap(copy);
        this.body = body == null ? new byte[0] : body;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns all response headers.
     *
     * @return an unmodifiable, case-insensitive header map
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns the first value of a header.
     *
     * @param name the header name, matched case-insensitively
     * @return the first value, or {@code null} when absent
     */
    public String header(String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /**
     * Returns the response body. The array is shared, do not modify it.
     *
     * @return the body bytes
     */
    public byte[] body() {
        return body;
    }

    /**
     * Returns the response body decoded as UTF-8.
     *
     * @return the body text
     */
    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
