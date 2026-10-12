package com.invoicexml.zugferdxrechnung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.invoicexml.zugferdxrechnung.model.AdditionalSupportingDocument;
import com.invoicexml.zugferdxrechnung.model.BuyerElectronicAddress;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceOptions;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.model.SellerContact;
import com.invoicexml.zugferdxrechnung.model.SellerElectronicAddress;
import com.invoicexml.zugferdxrechnung.model.XRechnungSyntax;
import com.invoicexml.zugferdxrechnung.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Calls the real API. Consumes credits, so it only runs when both the
 * {@code INVOICEXML_API_KEY} environment variable and {@code -Dinvoicexml.live=true} are set:
 * <pre>
 * mvn verify -Dinvoicexml.live=true
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "INVOICEXML_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "invoicexml.live", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveApiTest {

    private static final ZugferdXRechnungClient CLIENT = ZugferdXRechnungClient.builder()
            .apiKey(System.getenv("INVOICEXML_API_KEY"))
            .baseUrl(System.getProperty("invoicexml.baseUrl", ZugferdXRechnungClient.DEFAULT_BASE_URL))
            .build();

    private static byte[] zugferdPdf;
    private static String xrechnungUbl;
    private static String xrechnungCii;

    private static InvoiceDocument xrechnungInvoice() {
        InvoiceDocument invoice = Fixtures.invoice().setBuyerReference("04011000-12345-34");
        invoice.getSeller()
                .setContact(new SellerContact().setName("Max Mustermann").setPhone("+49 30 1234567")
                        .setEmail("rechnung@mustermann.de"))
                .setElectronicAddress(SellerElectronicAddress.of("rechnung@mustermann.de", "EM"));
        invoice.getBuyer().setElectronicAddress(BuyerElectronicAddress.of("einkauf@beispiel-handel.de", "EM"));
        return invoice;
    }

    private static String describe(ValidationResult r) {
        return r.getDetail() + " " + r.getErrors().stream().map(Object::toString).collect(Collectors.joining("; "));
    }

    private static void assertPdf(byte[] bytes) {
        assertTrue(bytes.length > 1000, "PDF too small: " + bytes.length);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @Order(1)
    void createZugferd() {
        zugferdPdf = CLIENT.createZugferd(Fixtures.invoice(), new InvoiceOptions().setLanguage(PdfLanguage.DE));
        assertPdf(zugferdPdf);
    }

    @Test
    @Order(2)
    void validateZugferd() {
        ValidationResult result = CLIENT.validateZugferd(FileUpload.ofPdf(zugferdPdf));
        assertTrue(result.isValid(), describe(result));
        assertTrue(result.getData().getHasEmbeddedXml());
        assertEquals("en16931", result.getData().getProfile());
        assertFalse(result.getReport().isEmpty());
    }

    @Test
    @Order(3)
    void extractJsonFromZugferd() {
        InvoiceDocument invoice = CLIENT.extractJson(FileUpload.ofPdf(zugferdPdf));
        assertEquals("RE-2026-001", invoice.getInvoiceNumber());
        assertEquals("EUR", invoice.getCurrency());
        assertEquals("DE", invoice.getSeller().getPostalAddress().getCountry());
        assertNotNull(invoice.getTotals().getGrandTotalAmount());
        assertEquals(0, invoice.getTotals().getGrandTotalAmount().compareTo(new java.math.BigDecimal("2975")));
    }

    @Test
    @Order(4)
    void createXRechnungUbl() {
        xrechnungUbl = CLIENT.createXRechnung(xrechnungInvoice());
        assertTrue(xrechnungUbl.contains("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"), xrechnungUbl);
        assertTrue(xrechnungUbl.contains("04011000-12345-34"));
    }

    @Test
    @Order(5)
    void validateXRechnung() {
        ValidationResult result = CLIENT.validateXRechnung(FileUpload.ofXml(xrechnungUbl));
        assertTrue(result.isValid(), describe(result));
        assertEquals("xrechnung", result.getData().getProfile());
    }

    @Test
    @Order(6)
    void createXRechnungCii() {
        xrechnungCii = CLIENT.createXRechnung(xrechnungInvoice(), new InvoiceOptions().setSyntax(XRechnungSyntax.CII));
        assertTrue(xrechnungCii.contains("CrossIndustryInvoice"), xrechnungCii);
        ValidationResult result = CLIENT.validateXRechnung(FileUpload.ofXml(xrechnungCii));
        assertTrue(result.isValid(), describe(result));
    }

    @Test
    @Order(7)
    void renderAndEmbed() {
        byte[] rendered = CLIENT.renderXRechnungToPdf(FileUpload.ofXml(xrechnungCii),
                new RenderOptions().setLanguage(PdfLanguage.DE));
        assertPdf(rendered);

        byte[] hybrid = CLIENT.embedZugferd(FileUpload.ofPdf(rendered), FileUpload.ofXml(xrechnungCii));
        assertPdf(hybrid);
        ValidationResult result = CLIENT.validateZugferd(FileUpload.ofPdf(hybrid));
        assertTrue(result.isValid(), describe(result));
    }

    @Test
    @Order(8)
    void extractAttachments() {
        InvoiceDocument invoice = xrechnungInvoice().addSupportingDocument(new AdditionalSupportingDocument()
                .setReference("LS-77")
                .setDescription("Lieferschein")
                .setAttachment("pos;menge\n1;10\n".getBytes(StandardCharsets.UTF_8))
                .setAttachmentMimeCode("text/csv")
                .setAttachmentFilename("lieferschein.csv"));
        String xml = CLIENT.createXRechnung(invoice);

        byte[] zip = CLIENT.extractAttachments(FileUpload.ofXml(xml));
        assertEquals("PK", new String(zip, 0, 2, StandardCharsets.US_ASCII));
    }

    @Test
    @Order(9)
    void rejectedInvoiceCarriesFindings() {
        InvoiceDocument invalid = Fixtures.invoice();
        invalid.getSeller().setVatIdentifier(null);

        InvoiceValidationException e = assertThrows(InvoiceValidationException.class,
                () -> CLIENT.createZugferd(invalid));
        assertEquals(400, e.getStatusCode());
        assertFalse(e.getErrors().isEmpty(), e.getMessage());
        assertNotNull(e.getErrors().get(0).getMessage());
    }

    @Test
    @Order(10)
    void invalidKeyIsRejected() {
        ZugferdXRechnungClient bad = ZugferdXRechnungClient.builder()
                .apiKey("invalid-key")
                .baseUrl(System.getProperty("invoicexml.baseUrl", ZugferdXRechnungClient.DEFAULT_BASE_URL))
                .build();
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> bad.validateXRechnung(FileUpload.ofXml("<x/>")));
        assertEquals(ErrorCodes.UNAUTHORIZED, e.getErrorCode());
    }
}
