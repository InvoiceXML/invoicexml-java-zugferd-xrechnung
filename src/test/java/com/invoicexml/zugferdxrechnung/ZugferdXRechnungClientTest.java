package com.invoicexml.zugferdxrechnung;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoicexml.zugferdxrechnung.email.EmailMessage;
import com.invoicexml.zugferdxrechnung.email.EmailSendResult;
import com.invoicexml.zugferdxrechnung.model.CreateInvoiceRequest;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceOptions;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.model.XRechnungSyntax;
import com.invoicexml.zugferdxrechnung.transport.TransportRequest;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import com.invoicexml.zugferdxrechnung.validation.ValidationReport;
import com.invoicexml.zugferdxrechnung.validation.ValidationResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZugferdXRechnungClientTest {

    private static final byte[] PDF = "%PDF-1.7 stub".getBytes(StandardCharsets.US_ASCII);
    private static final String XML = "<?xml version=\"1.0\"?><Invoice/>";
    private static final String ACCEPTED = "{\"status\":\"accepted\",\"messageId\":\"0107019a-msg\"}";

    private final StubTransport stub = new StubTransport();
    private final List<Duration> sleeps = new ArrayList<>();
    private final ObjectMapper json = new ObjectMapper();

    private ZugferdXRechnungClient client() {
        return ZugferdXRechnungClient.builder()
                .apiKey("test-key")
                .transport(stub)
                .sleeper(sleeps::add)
                .clock(Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC))
                .build();
    }

    private static String body(TransportRequest r) {
        return new String(r.body(), StandardCharsets.UTF_8);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static EmailMessage email() {
        return EmailMessage.of("invoices@example.com", "ap@example.org");
    }

    // ------------------------------------------------------------- create

    @Test
    void createZugferdPostsJsonAndReturnsPdf() throws Exception {
        stub.respond(200, "application/pdf", PDF);

        byte[] pdf = client().createZugferd(Fixtures.invoice());

        assertArrayEquals(PDF, pdf);
        TransportRequest r = stub.last();
        assertEquals("POST", r.method());
        assertEquals("https://api.invoicexml.com/v1/create/zugferd", r.uri().toString());
        assertEquals("Bearer test-key", r.header("Authorization"));
        assertEquals("application/json", r.header("Content-Type"));
        assertTrue(r.header("User-Agent").startsWith("invoicexml-java-zugferd-xrechnung/"));

        JsonNode sent = json.readTree(r.body());
        JsonNode invoice = sent.get("invoice");
        assertEquals("RE-2026-001", invoice.get("invoiceNumber").asText());
        assertEquals("2026-07-25", invoice.get("issueDate").asText());
        assertEquals("DE", invoice.at("/seller/postalAddress/country").asText());
        assertTrue(invoice.at("/lines/0/priceDetails/netPrice").isNumber());
        assertTrue(body(r).contains("\"netPrice\":250.00"), "BigDecimal scale must be kept");
        assertFalse(sent.has("options"), "null options must be omitted");
        assertFalse(invoice.has("dueDate"), "null fields must be omitted");
    }

    @Test
    void createZugferdSendsRenderingOptions() throws Exception {
        stub.respond(200, "application/pdf", PDF);

        client().createZugferd(Fixtures.invoice(), new InvoiceOptions()
                .setLanguage(PdfLanguage.DE)
                .setBrandColor("#0A7C66")
                .setIncludeAdvancedProperties(true));

        JsonNode options = json.readTree(stub.last().body()).get("options");
        assertEquals("DE", options.get("language").asText());
        assertEquals("#0A7C66", options.get("brandColor").asText());
        assertTrue(options.get("includeAdvancedProperties").asBoolean());
    }

    @Test
    void createXRechnungReturnsXmlAndSendsSyntax() throws Exception {
        stub.respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));

        String xml = client().createXRechnung(Fixtures.invoice().setBuyerReference("04011000-12345-34"),
                new InvoiceOptions().setSyntax(XRechnungSyntax.CII));

        assertEquals(XML, xml);
        assertEquals("/v1/create/xrechnung", stub.last().uri().getPath());
        JsonNode sent = json.readTree(stub.last().body());
        assertEquals("cii", sent.at("/options/syntax").asText());
        assertEquals("04011000-12345-34", sent.at("/invoice/buyerReference").asText());
    }

    @Test
    void createRejectionCarriesFindings() {
        stub.respondJson(400, Fixtures.resource("create-400.json"));

        InvoiceValidationException e = assertThrows(InvoiceValidationException.class,
                () -> client().createZugferd(Fixtures.invoice()));

        assertEquals(400, e.getStatusCode());
        assertEquals(ErrorCodes.MODEL_VALIDATION, e.getErrorCode());
        assertEquals("00-abc-def-01", e.getTraceId());
        assertEquals(1, e.getErrors().size());
        ValidationFinding f = e.getErrors().get(0);
        assertEquals("BR-CO-26", f.getRule());
        assertEquals(ValidationFinding.LAYER_EN16931, f.getLayer());
        assertEquals(List.of("seller.vatIdentifier"), f.getFields());
        assertEquals(List.of(), f.getAdditionalProperties().get("btCodes"));
        assertNull(f.getLine());
        assertEquals("380", e.getInvoiceData().getTypeCode());
        assertTrue(e.getMessage().contains("BR-CO-26"));
    }

    @Test
    void createRequiresAnInvoiceRequest() {
        assertThrows(NullPointerException.class,
                () -> client().createZugferd((com.invoicexml.zugferdxrechnung.model.CreateInvoiceRequest) null));
    }

    // ----------------------------------------------------------- validate

    @Test
    void validateZugferdUploadsFileAndParsesResult(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("incoming.pdf");
        Files.write(pdf, PDF);
        stub.respondJson(200, Fixtures.resource("validate-zugferd-valid.json"));

        ValidationResult result = client().validateZugferd(pdf);

        assertTrue(result.isValid());
        assertEquals("en16931", result.getData().getProfile());
        assertTrue(result.getData().getHasEmbeddedXml());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("invoiceNumber", result.getReport().get(0).getPath());
        assertTrue(result.getReport().get(0).isValid());

        TransportRequest r = stub.last();
        assertEquals("/v1/validate/zugferd", r.uri().getPath());
        assertEquals("application/json", r.header("Accept"));
        String contentType = r.header("Content-Type");
        assertTrue(contentType.startsWith("multipart/form-data; boundary="));
        String body = body(r);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"file\"; filename=\"incoming.pdf\""));
        assertTrue(body.contains("Content-Type: application/pdf"));
        assertTrue(body.contains("%PDF-1.7 stub"));
        String boundary = contentType.substring(contentType.indexOf('=') + 1);
        assertTrue(body.endsWith("--" + boundary + "--\r\n"));
    }

    @Test
    void validateXRechnungReportsInvalidInvoiceWithoutThrowing() {
        stub.respondJson(200, Fixtures.resource("validate-xrechnung-invalid.json"));

        ValidationResult result = client().validateXRechnung(FileUpload.ofXml(XML));

        assertFalse(result.isValid());
        assertFalse(result.getData().getSchematronValid());
        assertNull(result.getData().getHasEmbeddedXml());
        ValidationFinding error = result.getErrors().get(0);
        assertEquals("BR-DE-15", error.getRule());
        assertEquals(ValidationFinding.LAYER_CIUS, error.getLayer());
        assertEquals(Integer.valueOf(2), result.getWarnings().get(0).getLine());
        assertFalse(result.getReport().get(0).isValid());
        assertEquals("BR-DE-15", result.getReport().get(0).getErrors().get(0).getRule());
        assertEquals("/v1/validate/xrechnung", stub.last().uri().getPath());
        assertTrue(body(stub.last()).contains("filename=\"invoice.xml\""));
        assertTrue(body(stub.last()).contains("Content-Type: application/xml"));
    }

    // -------------------------------------------------------------- embed

    @Test
    void embedZugferdSendsPdfAndXmlParts() {
        stub.respond(200, "application/pdf", PDF);

        byte[] out = client().embedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML));

        assertArrayEquals(PDF, out);
        String body = body(stub.last());
        assertEquals("/v1/embed/zugferd", stub.last().uri().getPath());
        assertTrue(body.contains("name=\"pdf\"; filename=\"invoice.pdf\""));
        assertTrue(body.contains("name=\"xml\"; filename=\"invoice.xml\""));
        assertFalse(body.contains("skipValidation"));
    }

    @Test
    void embedZugferdCanSkipValidation() {
        stub.respond(200, "application/pdf", PDF);

        client().embedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), new EmbedOptions().setSkipValidation(true));

        assertTrue(body(stub.last()).contains("name=\"skipValidation\"\r\n\r\ntrue\r\n"));
    }

    // -------------------------------------------------------------- email

    @Test
    void emailZugferdPostsInvoiceAndEmailAndParsesResult() throws Exception {
        stub.respondJson(200, ACCEPTED);

        EmailSendResult result = client().emailZugferd(Fixtures.invoice(), email());

        assertEquals("accepted", result.getStatus());
        assertEquals("0107019a-msg", result.getMessageId());
        TransportRequest r = stub.last();
        assertEquals("POST", r.method());
        assertEquals("https://api.invoicexml.com/v1/create/zugferd/email", r.uri().toString());
        assertEquals("application/json", r.header("Content-Type"));
        assertEquals("application/json", r.header("Accept"));

        JsonNode sent = json.readTree(r.body());
        assertEquals("RE-2026-001", sent.at("/invoice/invoiceNumber").asText());
        assertFalse(sent.has("options"), "null options must be omitted");
        JsonNode email = sent.get("email");
        assertEquals(2, email.size(), "unset email fields must be omitted");
        assertEquals("invoices@example.com", email.get("from").asText());
        assertEquals("ap@example.org", email.get("to").asText());
    }

    @Test
    void emailZugferdSendsOptionsAndEveryEmailField() throws Exception {
        stub.respondJson(200, ACCEPTED);

        client().emailZugferd(Fixtures.invoice(), email()
                        .setFromName("Billing")
                        .addCc("cc@example.org")
                        .addBcc("bcc@example.org")
                        .setReplyTo("help@example.com")
                        .setSubject("Rechnung RE-2026-001")
                        .setMessage("Guten Tag"),
                new InvoiceOptions().setLanguage(PdfLanguage.DE));

        JsonNode sent = json.readTree(stub.last().body());
        assertEquals("DE", sent.at("/options/language").asText());
        JsonNode email = sent.get("email");
        assertEquals("Billing", email.get("fromName").asText());
        assertEquals("cc@example.org", email.at("/cc/0").asText());
        assertEquals("bcc@example.org", email.at("/bcc/0").asText());
        assertEquals("help@example.com", email.get("replyTo").asText());
        assertEquals("Rechnung RE-2026-001", email.get("subject").asText());
        assertEquals("Guten Tag", email.get("message").asText());
    }

    @Test
    void emailZugferdTakesAFullRequestEnvelope() throws Exception {
        stub.respondJson(200, ACCEPTED);

        client().emailZugferd(new CreateInvoiceRequest().setInvoice(Fixtures.invoice()), email());

        JsonNode sent = json.readTree(stub.last().body());
        assertEquals("RE-2026-001", sent.at("/invoice/invoiceNumber").asText());
        assertEquals("ap@example.org", sent.at("/email/to").asText());
    }

    @Test
    void emailXRechnungPostsToItsRouteAndSendsSyntax() throws Exception {
        stub.respondJson(200, ACCEPTED);

        EmailSendResult result = client().emailXRechnung(Fixtures.invoice(), email(),
                new InvoiceOptions().setSyntax(XRechnungSyntax.CII));

        assertEquals("0107019a-msg", result.getMessageId());
        assertEquals("/v1/create/xrechnung/email", stub.last().uri().getPath());
        JsonNode sent = json.readTree(stub.last().body());
        assertEquals("cii", sent.at("/options/syntax").asText());
        assertEquals("invoices@example.com", sent.at("/email/from").asText());
    }

    @Test
    void emailEmbeddedZugferdSendsFilePartsAndEmailFields() {
        stub.respondJson(200, ACCEPTED);

        EmailSendResult result = client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), email()
                .setFromName("Billing")
                .addCc("one@example.org")
                .addCc("two@example.org")
                .addBcc("three@example.org")
                .setReplyTo("help@example.com")
                .setSubject("Rechnung RE-2026-001")
                .setMessage("Guten Tag"));

        assertEquals("accepted", result.getStatus());
        assertEquals("0107019a-msg", result.getMessageId());
        TransportRequest r = stub.last();
        assertEquals("/v1/embed/zugferd/email", r.uri().getPath());
        assertEquals("application/json", r.header("Accept"));
        assertTrue(r.header("Content-Type").startsWith("multipart/form-data; boundary="));
        String body = body(r);
        assertTrue(body.contains("name=\"pdf\"; filename=\"invoice.pdf\""));
        assertTrue(body.contains("name=\"xml\"; filename=\"invoice.xml\""));
        assertTrue(body.contains("name=\"email.from\"\r\n\r\ninvoices@example.com\r\n"));
        assertTrue(body.contains("name=\"email.fromName\"\r\n\r\nBilling\r\n"));
        assertTrue(body.contains("name=\"email.to\"\r\n\r\nap@example.org\r\n"));
        assertEquals(2, count(body, "name=\"email.cc\"\r\n\r\n"));
        assertTrue(body.contains("name=\"email.cc\"\r\n\r\none@example.org\r\n"));
        assertTrue(body.contains("name=\"email.cc\"\r\n\r\ntwo@example.org\r\n"));
        assertEquals(1, count(body, "name=\"email.bcc\"\r\n\r\nthree@example.org\r\n"));
        assertTrue(body.contains("name=\"email.replyTo\"\r\n\r\nhelp@example.com\r\n"));
        assertTrue(body.contains("name=\"email.subject\"\r\n\r\nRechnung RE-2026-001\r\n"));
        assertTrue(body.contains("name=\"email.message\"\r\n\r\nGuten Tag\r\n"));
        assertFalse(body.contains("skipValidation"));
    }

    @Test
    void emailEmbeddedZugferdOmitsUnsetFields(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("rechnung.pdf");
        Path xml = dir.resolve("rechnung.xml");
        Files.write(pdf, PDF);
        Files.write(xml, XML.getBytes(StandardCharsets.UTF_8));
        stub.respondJson(200, ACCEPTED);

        client().emailEmbeddedZugferd(pdf, xml, email());

        String body = body(stub.last());
        assertTrue(body.contains("filename=\"rechnung.pdf\""));
        assertTrue(body.contains("filename=\"rechnung.xml\""));
        assertEquals(2, count(body, "name=\"email."));
        assertTrue(body.contains("name=\"email.from\""));
        assertTrue(body.contains("name=\"email.to\""));
    }

    @Test
    void emailRequiresAnEmailMessage() {
        assertThrows(NullPointerException.class, () -> client().emailZugferd(Fixtures.invoice(), null));
        assertThrows(NullPointerException.class,
                () -> client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), null));
        assertTrue(stub.requests.isEmpty());
    }

    @Test
    void unverifiedSenderDomainSurfacesItsErrorCode() {
        stub.respondJson(400, "{\"title\":\"Sending domain not verified\",\"detail\":\"The sending domain example.com is not verified yet.\",\"errorCode\":4022,\"traceId\":\"00-abc-def-01\"}");

        InvoiceXmlApiException e = assertThrows(InvoiceXmlApiException.class,
                () -> client().emailZugferd(Fixtures.invoice(), email()));

        assertFalse(e instanceof InvoiceValidationException);
        assertEquals(400, e.getStatusCode());
        assertEquals(ErrorCodes.EMAIL_DOMAIN_NOT_VERIFIED, e.getErrorCode());
        assertEquals("00-abc-def-01", e.getTraceId());
        assertEquals(1, stub.requests.size());
    }

    @Test
    void emailPlanAndAccountRefusalsSurfaceTheirErrorCodes() {
        stub.respondJson(403, "{\"title\":\"Email sending not included in your plan\",\"errorCode\":4020}")
            .respondJson(403, "{\"title\":\"Email sending disabled\",\"errorCode\":4021}");

        InvoiceXmlApiException plan = assertThrows(InvoiceXmlApiException.class,
                () -> client().emailXRechnung(Fixtures.invoice(), email()));
        InvoiceXmlApiException disabled = assertThrows(InvoiceXmlApiException.class,
                () -> client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), email()));

        assertEquals(ErrorCodes.EMAIL_NOT_IN_PLAN, plan.getErrorCode());
        assertEquals(ErrorCodes.EMAIL_SENDING_DISABLED, disabled.getErrorCode());
    }

    @Test
    void rejectedEmailFieldCarriesFindings() {
        stub.respondJson(400, "{\"title\":\"Validation failed\",\"errorCode\":4002,\"errors\":[{\"message\":\"The sender address is not valid.\",\"fields\":[\"email.from\"]}]}");

        InvoiceValidationException e = assertThrows(InvoiceValidationException.class,
                () -> client().emailZugferd(Fixtures.invoice(), email()));

        assertEquals(ErrorCodes.MODEL_VALIDATION, e.getErrorCode());
        assertEquals(List.of("email.from"), e.getErrors().get(0).getFields());
    }

    @Test
    void emailIsRetriedOnRateLimitWithTheSameBody() {
        stub.respondJson(429, "{\"title\":\"Too many requests\",\"errorCode\":4019}", "Retry-After", "2")
            .respondJson(200, ACCEPTED);

        EmailSendResult result = client().emailZugferd(Fixtures.invoice(), email());

        assertEquals("0107019a-msg", result.getMessageId());
        assertEquals(List.of(Duration.ofSeconds(2)), sleeps);
        assertEquals(2, stub.requests.size());
        assertArrayEquals(stub.requests.get(0).body(), stub.requests.get(1).body());
    }

    @Test
    void emailIsNotRetriedOnServerOrNetworkFailure() {
        stub.respondJson(503, "{\"title\":\"Service unavailable\"}");
        assertThrows(InvoiceXmlApiException.class, () -> client().emailZugferd(Fixtures.invoice(), email()));
        assertEquals(1, stub.requests.size());

        stub.fail(new IOException("connection reset"));
        assertThrows(InvoiceXmlConnectionException.class,
                () -> client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), email()));
        assertEquals(2, stub.requests.size());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void blankCopyAddressesAreDroppedOnBothRouteTypes() throws Exception {
        stub.respondJson(200, ACCEPTED).respondJson(200, ACCEPTED);
        EmailMessage email = email().setCc(List.of(" ", " one@example.org ")).setBcc(List.of(""));

        client().emailZugferd(Fixtures.invoice(), email);
        client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), email);

        JsonNode sent = json.readTree(stub.requests.get(0).body()).get("email");
        assertEquals(1, sent.get("cc").size());
        assertEquals("one@example.org", sent.at("/cc/0").asText());
        assertFalse(sent.has("bcc"), "a list left empty must be omitted");
        String form = body(stub.requests.get(1));
        assertEquals(1, count(form, "name=\"email.cc\""));
        assertTrue(form.contains("name=\"email.cc\"\r\n\r\none@example.org\r\n"));
        assertFalse(form.contains("name=\"email.bcc\""));
    }

    @Test
    void emailEmbeddedZugferdSendsNonAsciiSubjectAndSenderNameAsUtf8() {
        stub.respondJson(200, ACCEPTED);

        client().emailEmbeddedZugferd(FileUpload.ofPdf(PDF), FileUpload.ofXml(XML), email()
                .setFromName("Müller & Söhne")
                .setSubject("Rechnung für März"));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"email.fromName\"\r\n\r\nMüller & Söhne\r\n"));
        assertTrue(body.contains("name=\"email.subject\"\r\n\r\nRechnung für März\r\n"));
    }

    // ------------------------------------------------------------- render

    @Test
    void renderXRechnungSendsLanguageAndLogo() {
        stub.respond(200, "application/pdf", PDF);

        client().renderXRechnungToPdf(FileUpload.ofXml(XML),
                new RenderOptions().setLanguage(PdfLanguage.DE).setLogoUrl("https://example.com/logo.png"));

        String body = body(stub.last());
        assertEquals("/v1/render/xrechnung/to/pdf", stub.last().uri().getPath());
        assertTrue(body.contains("name=\"language\"\r\n\r\nde\r\n"));
        assertTrue(body.contains("name=\"logoUrl\"\r\n\r\nhttps://example.com/logo.png\r\n"));
    }

    @Test
    void renderCiiPostsToItsRouteAndReturnsPdf(@TempDir Path dir) throws Exception {
        Path xml = dir.resolve("rechnung.xml");
        Files.write(xml, XML.getBytes(StandardCharsets.UTF_8));
        stub.respond(200, "application/pdf", PDF);

        assertArrayEquals(PDF, client().renderCiiToPdf(xml));

        assertEquals("/v1/render/cii/to/pdf", stub.last().uri().getPath());
        String body = body(stub.last());
        assertTrue(body.contains("name=\"file\"; filename=\"rechnung.xml\""));
        assertEquals(1, count(body, "form-data; name="), "unset options must be omitted");
    }

    @Test
    void renderSendsQrCodeOnlyWhenSet() {
        stub.respond(200, "application/pdf", PDF).respond(200, "application/pdf", PDF).respond(200, "application/pdf", PDF);

        client().renderCiiToPdf(FileUpload.ofXml(XML), new RenderOptions().setIncludeQrCode(true));
        client().renderXRechnungToPdf(FileUpload.ofXml(XML), new RenderOptions().setIncludeQrCode(false));
        client().renderCiiToPdf(FileUpload.ofXml(XML), new RenderOptions());

        assertTrue(body(stub.requests.get(0)).contains("name=\"includeQrCode\"\r\n\r\ntrue\r\n"));
        assertTrue(body(stub.requests.get(1)).contains("name=\"includeQrCode\"\r\n\r\nfalse\r\n"));
        assertFalse(body(stub.requests.get(2)).contains("includeQrCode"));
    }

    // ------------------------------------------------------------ convert

    private void assertConvertsToXml(String route, Supplier<String> call) {
        stub.respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));

        assertEquals(XML, call.get());

        assertEquals(route, stub.last().uri().getPath());
        String body = body(stub.last());
        assertTrue(body.contains("form-data; name=\"file\"; filename="));
        assertEquals(1, count(body, "form-data; name="), "unset options must be omitted");
    }

    private void assertConvertsToPdf(String route, Supplier<byte[]> call) {
        stub.respond(200, "application/pdf", PDF);

        assertArrayEquals(PDF, call.get());

        assertEquals(route, stub.last().uri().getPath());
        String body = body(stub.last());
        assertTrue(body.contains("form-data; name=\"file\"; filename="));
        assertEquals(1, count(body, "form-data; name="), "unset options must be omitted");
    }

    @Test
    void convertRoutesToXmlPostTheFileAndReturnXml() {
        ZugferdXRechnungClient c = client();
        FileUpload pdf = FileUpload.ofPdf(PDF);
        FileUpload xml = FileUpload.ofXml(XML);

        assertConvertsToXml("/v1/convert/zugferd/to/ubl", () -> c.convertZugferdToUbl(pdf));
        assertConvertsToXml("/v1/convert/zugferd/to/cii", () -> c.convertZugferdToCii(pdf));
        assertConvertsToXml("/v1/convert/xrechnung/to/ubl", () -> c.convertXRechnungToUbl(xml));
        assertConvertsToXml("/v1/convert/xrechnung/to/cii", () -> c.convertXRechnungToCii(xml));
        assertConvertsToXml("/v1/convert/ubl/to/xrechnung", () -> c.convertUblToXRechnung(xml));
        assertConvertsToXml("/v1/convert/cii/to/xrechnung", () -> c.convertCiiToXRechnung(xml));
    }

    @Test
    void convertRoutesToZugferdPostTheFileAndReturnPdf() {
        ZugferdXRechnungClient c = client();
        FileUpload xml = FileUpload.ofXml(XML);

        assertConvertsToPdf("/v1/convert/cii/to/zugferd", () -> c.convertCiiToZugferd(xml));
        assertConvertsToPdf("/v1/convert/ubl/to/zugferd", () -> c.convertUblToZugferd(xml));
        assertConvertsToPdf("/v1/convert/xrechnung/to/zugferd", () -> c.convertXRechnungToZugferd(xml));
    }

    @Test
    void convertTakesPaths(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("rechnung.pdf");
        Path xml = dir.resolve("rechnung.xml");
        Files.write(pdf, PDF);
        Files.write(xml, XML.getBytes(StandardCharsets.UTF_8));
        ZugferdXRechnungClient c = client();

        assertConvertsToXml("/v1/convert/zugferd/to/cii", () -> c.convertZugferdToCii(pdf));
        assertTrue(body(stub.last()).contains("filename=\"rechnung.pdf\""));
        assertConvertsToPdf("/v1/convert/xrechnung/to/zugferd", () -> c.convertXRechnungToZugferd(xml));
        assertTrue(body(stub.last()).contains("filename=\"rechnung.xml\""));
    }

    @Test
    void convertToXmlSendsValidationLanguageAndNoLanguage() {
        stub.respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));

        client().convertUblToXRechnung(FileUpload.ofXml(XML), new ConvertOptions().setValidationLanguage(PdfLanguage.DE));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"validationLanguage\"\r\n\r\nde\r\n"));
        assertFalse(body.contains("name=\"language\""));
    }

    @Test
    void convertToZugferdSendsLanguageAndValidationLanguage() {
        stub.respond(200, "application/pdf", PDF);

        client().convertCiiToZugferd(FileUpload.ofXml(XML), new ConvertToPdfOptions()
                .setLanguage(PdfLanguage.DE)
                .setValidationLanguage(PdfLanguage.FR));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"language\"\r\n\r\nde\r\n"));
        assertTrue(body.contains("name=\"validationLanguage\"\r\n\r\nfr\r\n"));
    }

    @Test
    void convertRejectionCarriesFindings() {
        stub.respondJson(400, Fixtures.resource("create-400.json"));

        InvoiceValidationException e = assertThrows(InvoiceValidationException.class,
                () -> client().convertCiiToXRechnung(FileUpload.ofXml(XML)));

        assertEquals(ErrorCodes.MODEL_VALIDATION, e.getErrorCode());
        assertEquals("BR-CO-26", e.getErrors().get(0).getRule());
    }

    @Test
    void convertOfAPdfWithoutXmlSurfacesItsErrorCode() {
        stub.respondJson(400, "{\"title\":\"No embedded XML\",\"errorCode\":4006}");

        InvoiceXmlApiException e = assertThrows(InvoiceXmlApiException.class,
                () -> client().convertZugferdToUbl(FileUpload.ofPdf(PDF)));

        assertEquals(ErrorCodes.NO_EMBEDDED_XML, e.getErrorCode());
    }

    // ------------------------------------------------------------- report

    @Test
    void validateZugferdReportReturnsPdfAndPassedVerdict(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("incoming.pdf");
        Files.write(pdf, PDF);
        stub.respond(200, "application/pdf", PDF, "X-Invoice-Valid", "true");

        ValidationReport report = client().validateZugferdReport(pdf);

        assertArrayEquals(PDF, report.getFile());
        assertEquals(Boolean.TRUE, report.getValid());
        assertEquals("/v1/validate/zugferd/report", stub.last().uri().getPath());
        String body = body(stub.last());
        assertTrue(body.contains("name=\"file\"; filename=\"incoming.pdf\""));
        assertEquals(1, count(body, "form-data; name="), "unset options must be omitted");
    }

    @Test
    void validateXRechnungReportReturnsFailedVerdictWithoutThrowing() {
        stub.respond(200, "application/pdf", PDF, "X-Invoice-Valid", "false");

        ValidationReport report = client().validateXRechnungReport(FileUpload.ofXml(XML));

        assertArrayEquals(PDF, report.getFile());
        assertEquals(Boolean.FALSE, report.getValid());
        assertEquals("/v1/validate/xrechnung/report", stub.last().uri().getPath());
    }

    @Test
    void reportVerdictIsNullWhenTheHeaderIsMissingOrUnreadable() {
        stub.respond(200, "application/pdf", PDF).respond(200, "application/pdf", PDF, "X-Invoice-Valid", "maybe");

        assertNull(client().validateZugferdReport(FileUpload.ofPdf(PDF)).getValid());
        assertNull(client().validateZugferdReport(FileUpload.ofPdf(PDF)).getValid());
    }

    @Test
    void reportHeaderNameIsMatchedCaseInsensitively() {
        stub.respond(200, "application/pdf", PDF, "x-invoice-valid", "true");

        assertEquals(Boolean.TRUE, client().validateZugferdReport(FileUpload.ofPdf(PDF)).getValid());
    }

    @Test
    void reportSendsValidationLanguage() {
        stub.respond(200, "application/pdf", PDF, "X-Invoice-Valid", "true")
            .respond(200, "application/pdf", PDF, "X-Invoice-Valid", "true");

        client().validateZugferdReport(FileUpload.ofPdf(PDF), new ReportOptions().setValidationLanguage(PdfLanguage.DE));
        client().validateXRechnungReport(FileUpload.ofXml(XML), new ReportOptions().setValidationLanguage(PdfLanguage.FR));

        assertTrue(body(stub.requests.get(0)).contains("name=\"validationLanguage\"\r\n\r\nde\r\n"));
        assertTrue(body(stub.requests.get(1)).contains("name=\"validationLanguage\"\r\n\r\nfr\r\n"));
    }

    @Test
    void reportTransportProblemRaisesTheUsualException() {
        stub.respondJson(400, "{\"title\":\"Missing file\",\"errorCode\":4003}");

        InvoiceXmlApiException e = assertThrows(InvoiceXmlApiException.class,
                () -> client().validateZugferdReport(FileUpload.ofPdf(new byte[0])));

        assertEquals(ErrorCodes.MISSING_FILE, e.getErrorCode());
    }

    // ---------------------------------------------------------- transform

    @Test
    void transformToZugferdOmitsUnsetOptions(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("invoice.pdf");
        Files.write(pdf, PDF);
        stub.respond(200, "application/pdf", PDF).respond(200, "application/pdf", PDF);

        assertArrayEquals(PDF, client().transformToZugferd(pdf));
        client().transformToZugferd(FileUpload.ofPdf(PDF), new TransformOptions());

        assertEquals("/v1/transform/to/zugferd", stub.last().uri().getPath());
        for (TransportRequest r : stub.requests) {
            assertEquals(1, count(body(r), "form-data; name="), "unset options must be omitted");
        }
    }

    @Test
    void transformToZugferdSendsEveryOption() {
        stub.respond(200, "application/pdf", PDF);

        client().transformToZugferd(FileUpload.ofPdf(PDF), new TransformOptions()
                .setLanguage(PdfLanguage.DE)
                .setPreserveOriginalPdf(true)
                .setIncludeAdvancedProperties(true)
                .setValidationLanguage(PdfLanguage.FR));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"language\"\r\n\r\nde\r\n"));
        assertTrue(body.contains("name=\"preserveOriginalPdf\"\r\n\r\ntrue\r\n"));
        assertTrue(body.contains("name=\"includeAdvancedProperties\"\r\n\r\ntrue\r\n"));
        assertTrue(body.contains("name=\"validationLanguage\"\r\n\r\nfr\r\n"));
    }

    @Test
    void transformCanTurnOffTheDetailsSection() {
        stub.respond(200, "application/pdf", PDF);

        client().transformToZugferd(FileUpload.ofPdf(PDF), new TransformOptions().setIncludeAdvancedProperties(false));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"includeAdvancedProperties\"\r\n\r\nfalse\r\n"));
        assertFalse(body.contains("preserveOriginalPdf"));
    }

    @Test
    void transformToXRechnungSendsBuyerReferenceAndReturnsXml(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("invoice.pdf");
        Files.write(pdf, PDF);
        stub.respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));

        assertEquals(XML, client().transformToXRechnung(pdf, "04011000-12345-34"));

        assertEquals("/v1/transform/to/xrechnung", stub.last().uri().getPath());
        String body = body(stub.last());
        assertTrue(body.contains("name=\"buyerReference\"\r\n\r\n04011000-12345-34\r\n"));
        assertEquals(2, count(body, "form-data; name="), "unset options must be omitted");
    }

    @Test
    void transformToXRechnungSendsValidationLanguageAndNoLanguage() {
        stub.respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));

        client().transformToXRechnung(FileUpload.ofPdf(PDF), "04011000-12345-34",
                new TransformToXRechnungOptions().setValidationLanguage(PdfLanguage.DE));

        String body = body(stub.last());
        assertTrue(body.contains("name=\"validationLanguage\"\r\n\r\nde\r\n"));
        assertFalse(body.contains("name=\"language\""));
    }

    @Test
    void transformToXRechnungRequiresABuyerReference() {
        for (String buyerReference : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> client().transformToXRechnung(FileUpload.ofPdf(PDF), buyerReference));
        }
        assertTrue(stub.requests.isEmpty());
    }

    @Test
    void transformRateLimitMakesOneRequestAndThrows() {
        String limited = "{\"title\":\"Too many requests\",\"errorCode\":4019}";
        stub.respondJson(429, limited, "Retry-After", "2").respondJson(429, limited, "Retry-After", "2");

        RateLimitException zugferd = assertThrows(RateLimitException.class,
                () -> client().transformToZugferd(FileUpload.ofPdf(PDF)));
        RateLimitException xrechnung = assertThrows(RateLimitException.class,
                () -> client().transformToXRechnung(FileUpload.ofPdf(PDF), "04011000-12345-34"));

        assertEquals(2, stub.requests.size(), "one request per call");
        assertEquals("/v1/transform/to/zugferd", stub.requests.get(0).uri().getPath());
        assertEquals("/v1/transform/to/xrechnung", stub.requests.get(1).uri().getPath());
        assertTrue(sleeps.isEmpty());
        assertEquals(Duration.ofSeconds(2), zugferd.getRetryAfter());
        assertEquals(Duration.ofSeconds(2), xrechnung.getRetryAfter());
        assertEquals(ErrorCodes.TOO_MANY_REQUESTS, zugferd.getErrorCode());
    }

    @Test
    void transformRejectionCarriesFindings() {
        stub.respondJson(400, Fixtures.resource("create-400.json"));

        InvoiceValidationException e = assertThrows(InvoiceValidationException.class,
                () -> client().transformToZugferd(FileUpload.ofPdf(PDF)));

        assertEquals(ErrorCodes.MODEL_VALIDATION, e.getErrorCode());
    }

    // ------------------------------------------------------------ extract

    @Test
    void extractJsonReturnsTheInvoiceModel() {
        stub.respondJson(200, Fixtures.resource("extract-json.json"));

        InvoiceDocument invoice = client().extractJson(FileUpload.ofPdf(PDF));

        assertEquals("/v1/extract/json", stub.last().uri().getPath());
        assertEquals("RE-2026-001", invoice.getInvoiceNumber());
        assertEquals(LocalDate.of(2026, 7, 25), invoice.getIssueDate());
        assertEquals("380", invoice.getTypeCode());
        assertEquals("Plain note", invoice.getNotes().get(0).getNote());
        assertNull(invoice.getNotes().get(0).getSubjectCode());
        assertEquals("AAB", invoice.getNotes().get(1).getSubjectCode());
        assertEquals("0088", invoice.getSeller().getIdentifiers().get(0).getSchemeId());
        assertEquals("EM", invoice.getSeller().getElectronicAddress().getSchemeId());
        assertEquals("58", invoice.getPaymentDetails().getPaymentMeansCode());
        assertEquals(0, new BigDecimal("2975.00").compareTo(invoice.getTotals().getGrandTotalAmount()));
        assertEquals("S", invoice.getVatBreakdowns().get(0).getCategoryCode());
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII),
                invoice.getSupportingDocuments().get(0).getAttachment());
    }

    @Test
    void extractAttachmentsReturnsZip() {
        byte[] zip = {0x50, 0x4b, 0x03, 0x04};
        stub.respond(200, "application/zip", zip);

        assertArrayEquals(zip, client().extractAttachments(FileUpload.ofXml(XML)));
        assertEquals("/v1/extract/attachments", stub.last().uri().getPath());
    }

    // ------------------------------------------------------------ retries

    @Test
    void retriesAfterRateLimitUsingRetryAfterSeconds() {
        stub.respondJson(429, "{\"title\":\"Too many requests\",\"errorCode\":4019}", "Retry-After", "2")
            .respond(200, "application/pdf", PDF);

        assertArrayEquals(PDF, client().createZugferd(Fixtures.invoice()));
        assertEquals(List.of(Duration.ofSeconds(2)), sleeps);
        assertEquals(2, stub.requests.size());
        assertArrayEquals(stub.requests.get(0).body(), stub.requests.get(1).body(), "retry must resend the same body");
    }

    @Test
    void retryAfterHttpDateIsHonoured() {
        stub.respondJson(429, "{}", "Retry-After", "Fri, 09 Oct 2026 12:00:05 GMT")
            .respond(200, "application/pdf", PDF);

        client().createZugferd(Fixtures.invoice());

        assertEquals(List.of(Duration.ofSeconds(5)), sleeps);
    }

    @Test
    void renderConvertAndReportRetryAfterRateLimit() {
        stub.respondJson(429, "{}", "Retry-After", "1").respond(200, "application/pdf", PDF);
        stub.respondJson(429, "{}", "Retry-After", "1").respond(200, "application/xml", XML.getBytes(StandardCharsets.UTF_8));
        stub.respondJson(429, "{}", "Retry-After", "1").respond(200, "application/pdf", PDF, "X-Invoice-Valid", "true");

        client().renderCiiToPdf(FileUpload.ofXml(XML));
        client().convertZugferdToUbl(FileUpload.ofPdf(PDF));
        client().validateZugferdReport(FileUpload.ofPdf(PDF));

        assertEquals(List.of(
                "/v1/render/cii/to/pdf", "/v1/render/cii/to/pdf",
                "/v1/convert/zugferd/to/ubl", "/v1/convert/zugferd/to/ubl",
                "/v1/validate/zugferd/report", "/v1/validate/zugferd/report"),
            stub.requests.stream().map(request -> request.uri().getPath()).collect(Collectors.toList()));
        assertEquals(3, sleeps.size());
    }

    @Test
    void rateLimitSurfacesAfterRetriesAreUsedUp() {
        for (int i = 0; i < 4; i++) {
            stub.respondJson(429, "{\"title\":\"Too many requests\",\"errorCode\":4019}", "Retry-After", "1");
        }

        RateLimitException e = assertThrows(RateLimitException.class, () -> client().createZugferd(Fixtures.invoice()));

        assertEquals(4, stub.requests.size());
        assertEquals(3, sleeps.size());
        assertEquals(Duration.ofSeconds(1), e.getRetryAfter());
        assertEquals(ErrorCodes.TOO_MANY_REQUESTS, e.getErrorCode());
    }

    @Test
    void retryAfterLongerThanTheCapFailsFast() {
        stub.respondJson(429, "{}", "Retry-After", "600");

        assertThrows(RateLimitException.class, () -> client().createZugferd(Fixtures.invoice()));
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void missingRetryAfterFallsBackToBackoff() {
        stub.respondJson(429, "{}").respondJson(429, "{}").respond(200, "application/pdf", PDF);

        client().createZugferd(Fixtures.invoice());

        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeps);
    }

    // ------------------------------------------------------------- errors

    @Test
    void unauthorizedMapsToAuthenticationException() {
        stub.respondJson(401, "{\"title\":\"Unauthorized\",\"detail\":\"Invalid or inactive API key.\",\"errorCode\":4010}");

        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> client().validateXRechnung(FileUpload.ofXml(XML)));
        assertEquals(ErrorCodes.UNAUTHORIZED, e.getErrorCode());
        assertEquals("Invalid or inactive API key.", e.getDetail());
    }

    @Test
    void payloadTooLargeMapsToItsOwnException() {
        stub.respondJson(413, "{\"title\":\"Payload too large\",\"errorCode\":4005}");

        assertThrows(PayloadTooLargeException.class, () -> client().validateZugferd(FileUpload.ofPdf(PDF)));
    }

    @Test
    void otherProblemsMapToApiException() {
        stub.respondJson(400, "{\"title\":\"Missing file\",\"detail\":\"No PDF file was provided in the request.\",\"errorCode\":4003}");

        InvoiceXmlApiException e = assertThrows(InvoiceXmlApiException.class,
                () -> client().validateZugferd(FileUpload.ofPdf(new byte[0])));
        assertFalse(e instanceof InvoiceValidationException);
        assertEquals(ErrorCodes.MISSING_FILE, e.getErrorCode());
        assertTrue(e.getMessage().contains("Missing file"));
    }

    @Test
    void nonJsonErrorBodyStillProducesAnException() {
        stub.respond(500, "text/html", "<html>oops</html>".getBytes(StandardCharsets.UTF_8));

        InvoiceXmlApiException e = assertThrows(InvoiceXmlApiException.class,
                () -> client().createZugferd(Fixtures.invoice()));
        assertEquals(500, e.getStatusCode());
        assertNull(e.getErrorCode());
        assertEquals("<html>oops</html>", e.getResponseBody());
    }

    @Test
    void ioFailureMapsToConnectionException() {
        IOException cause = new IOException("connection reset");
        stub.fail(cause);

        InvoiceXmlConnectionException e = assertThrows(InvoiceXmlConnectionException.class,
                () -> client().createZugferd(Fixtures.invoice()));
        assertSame(cause, e.getCause());
    }

    // ------------------------------------------------------------ builder

    @Test
    void builderRequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () -> ZugferdXRechnungClient.builder().build());
        assertThrows(IllegalArgumentException.class, () -> ZugferdXRechnungClient.create(" "));
    }

    @Test
    void baseUrlTrailingSlashIsIgnored() {
        stub.respond(200, "application/pdf", PDF);
        ZugferdXRechnungClient c = ZugferdXRechnungClient.builder()
                .apiKey("k").baseUrl("http://localhost:5000/").transport(stub).build();

        c.createZugferd(Fixtures.invoice());

        assertEquals("http://localhost:5000/v1/create/zugferd", stub.last().uri().toString());
    }

    @Test
    void versionIsFilteredFromThePom() {
        assertFalse(ZugferdXRechnungClient.VERSION.startsWith("$"));
    }
}
