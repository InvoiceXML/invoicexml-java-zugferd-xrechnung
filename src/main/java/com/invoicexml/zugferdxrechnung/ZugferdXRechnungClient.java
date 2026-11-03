package com.invoicexml.zugferdxrechnung;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoicexml.zugferdxrechnung.email.EmailMessage;
import com.invoicexml.zugferdxrechnung.email.EmailSendResult;
import com.invoicexml.zugferdxrechnung.model.CreateInvoiceRequest;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceOptions;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.model.UblProfile;
import com.invoicexml.zugferdxrechnung.parse.ParsedInvoice;
import com.invoicexml.zugferdxrechnung.transport.HttpTransport;
import com.invoicexml.zugferdxrechnung.transport.JdkHttpTransport;
import com.invoicexml.zugferdxrechnung.transport.TransportRequest;
import com.invoicexml.zugferdxrechnung.transport.TransportResponse;
import com.invoicexml.zugferdxrechnung.validation.ValidationReport;
import com.invoicexml.zugferdxrechnung.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Client for the ZUGFeRD and XRechnung endpoints of the InvoiceXML API.
 *
 * <pre>{@code
 * ZugferdXRechnungClient client = ZugferdXRechnungClient.create(System.getenv("INVOICEXML_API_KEY"));
 *
 * byte[] pdf = client.createZugferd(invoice);              // ZUGFeRD PDF/A-3
 * String xml = client.createXRechnung(invoice);            // XRechnung XML (UBL)
 * ValidationResult result = client.validateZugferd(Path.of("incoming.pdf"));
 * }</pre>
 *
 * <p>Every create call validates the invoice against the full rule set before
 * the document is returned. A rejected invoice raises an
 * {@link InvoiceValidationException} listing the violated rules.
 *
 * <p>HTTP 429 responses are retried automatically after the {@code Retry-After}
 * delay, except on transform and parse calls. Other errors raise an {@link InvoiceXmlApiException} subclass, and
 * network failures raise an {@link InvoiceXmlConnectionException}. All are unchecked.
 *
 * <p>Instances are immutable and thread-safe. Create one and share it.
 */
public final class ZugferdXRechnungClient {

    /** The production API endpoint. */
    public static final String DEFAULT_BASE_URL = "https://api.invoicexml.com";

    /** This library's version. */
    public static final String VERSION = loadVersion();

    private final String apiKey;
    private final String baseUrl;
    private final HttpTransport transport;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration maxRetryWait;
    private final Sleeper sleeper;
    private final Clock clock;
    private final String userAgent;
    private final ObjectMapper mapper = Json.newMapper();

    private ZugferdXRechnungClient(Builder b) {
        this.apiKey = b.apiKey;
        this.baseUrl = b.baseUrl;
        this.transport = b.transport != null ? b.transport : new JdkHttpTransport(b.connectTimeout);
        this.timeout = b.timeout;
        this.maxRetries = b.maxRetries;
        this.maxRetryWait = b.maxRetryWait;
        this.sleeper = b.sleeper;
        this.clock = b.clock;
        this.userAgent = "invoicexml-java-zugferd-xrechnung/" + VERSION
                + " (Java " + System.getProperty("java.version", "unknown") + ")";
    }

    /**
     * Creates a client for the production API with default settings.
     *
     * @param apiKey your InvoiceXML API key
     * @return the client
     */
    public static ZugferdXRechnungClient create(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    /**
     * Returns a builder for a client with custom settings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ---------------------------------------------------------------- create

    /**
     * Creates a ZUGFeRD hybrid invoice: a PDF/A-3 with the CII XML embedded.
     *
     * <p>The profile is EN 16931 unless the invoice's {@code specificationId}
     * declares another official ZUGFeRD profile. Declaring the XRechnung
     * URN selects the XRECHNUNG reference profile. Totals and the VAT breakdown
     * are calculated from the lines when omitted.
     *
     * @param invoice the invoice
     * @return the PDF bytes
     * @throws InvoiceValidationException when the invoice violates the rule set
     */
    public byte[] createZugferd(InvoiceDocument invoice) {
        return createZugferd(new CreateInvoiceRequest().setInvoice(invoice));
    }

    /**
     * Creates a ZUGFeRD hybrid invoice with rendering options such as language,
     * logo, brand colour or your own PDF as the visual layer.
     *
     * @param invoice the invoice
     * @param options the options, or {@code null} for the defaults
     * @return the PDF bytes
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createZugferd(InvoiceDocument)
     */
    public byte[] createZugferd(InvoiceDocument invoice, InvoiceOptions options) {
        return createZugferd(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options));
    }

    /**
     * Creates a ZUGFeRD hybrid invoice from a full request envelope.
     *
     * @param request the request
     * @return the PDF bytes
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createZugferd(InvoiceDocument)
     */
    public byte[] createZugferd(CreateInvoiceRequest request) {
        return postJson("/v1/create/zugferd", request, null).body();
    }

    /**
     * Creates an XRechnung in UBL 2.1 syntax.
     *
     * <p>The XRechnung rules apply on top of EN 16931: the Leitweg-ID goes in
     * {@code buyerReference}, and seller contact and electronic addresses
     * are required.
     *
     * @param invoice the invoice
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     */
    public String createXRechnung(InvoiceDocument invoice) {
        return createXRechnung(new CreateInvoiceRequest().setInvoice(invoice));
    }

    /**
     * Creates an XRechnung with options. Set
     * {@link InvoiceOptions#setSyntax(com.invoicexml.zugferdxrechnung.model.XRechnungSyntax)}
     * to {@code CII} for the UN/CEFACT CII syntax instead of UBL.
     *
     * @param invoice the invoice
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createXRechnung(InvoiceDocument)
     */
    public String createXRechnung(InvoiceDocument invoice, InvoiceOptions options) {
        return createXRechnung(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options));
    }

    /**
     * Creates an XRechnung from a full request envelope.
     *
     * @param request the request
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createXRechnung(InvoiceDocument)
     */
    public String createXRechnung(CreateInvoiceRequest request) {
        return new String(postJson("/v1/create/xrechnung", request, null).body(), StandardCharsets.UTF_8);
    }

    /**
     * Creates a plain UN/CEFACT CII invoice XML, validated with the full rule set.
     *
     * <p>The XML carries the EN 16931 guideline identifier, so it embeds straight
     * into your own PDF with {@link #embedZugferd(FileUpload, FileUpload)}. Totals
     * and the VAT breakdown are calculated from the lines when omitted.
     *
     * @param invoice the invoice
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     */
    public String createCii(InvoiceDocument invoice) {
        return createCii(new CreateInvoiceRequest().setInvoice(invoice));
    }

    /**
     * Creates a plain CII invoice XML with options, such as the language of the
     * default texts. The PDF rendering options do not apply.
     *
     * @param invoice the invoice
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createCii(InvoiceDocument)
     */
    public String createCii(InvoiceDocument invoice, InvoiceOptions options) {
        return createCii(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options));
    }

    /**
     * Creates a plain CII invoice XML from a full request envelope.
     *
     * @param request the request
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createCii(InvoiceDocument)
     */
    public String createCii(CreateInvoiceRequest request) {
        return new String(postJson("/v1/create/cii", request, null).body(), StandardCharsets.UTF_8);
    }

    /**
     * Creates a UBL 2.1 invoice XML, validated with the rule set of its profile.
     *
     * <p>The profile is Peppol BIS Billing 3.0 unless the options set another
     * {@link UblProfile}. Totals and the VAT breakdown are calculated from the
     * lines when omitted.
     *
     * @param invoice the invoice
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     */
    public String createUbl(InvoiceDocument invoice) {
        return createUbl(new CreateInvoiceRequest().setInvoice(invoice));
    }

    /**
     * Creates a UBL 2.1 invoice XML with options, such as the {@link UblProfile}
     * or the language of the default texts. The PDF rendering options do not apply.
     *
     * @param invoice the invoice
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createUbl(InvoiceDocument)
     */
    public String createUbl(InvoiceDocument invoice, InvoiceOptions options) {
        return createUbl(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options));
    }

    /**
     * Creates a UBL 2.1 invoice XML from a full request envelope.
     *
     * @param request the request
     * @return the XML document
     * @throws InvoiceValidationException when the invoice violates the rule set
     * @see #createUbl(InvoiceDocument)
     */
    public String createUbl(CreateInvoiceRequest request) {
        return new String(postJson("/v1/create/ubl", request, null).body(), StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------- validate

    /**
     * Validates a ZUGFeRD PDF. The embedded XML is extracted, its profile detected
     * from the declared specification identifier, and the official XSD and Schematron rules for that profile applied.
     *
     * @param pdf the PDF file
     * @return the result; check {@link ValidationResult#isValid()}
     */
    public ValidationResult validateZugferd(Path pdf) {
        return validateZugferd(FileUpload.of(pdf));
    }

    /**
     * Validates a ZUGFeRD PDF.
     *
     * @param pdf the PDF upload
     * @return the result; check {@link ValidationResult#isValid()}
     * @see #validateZugferd(Path)
     */
    public ValidationResult validateZugferd(FileUpload pdf) {
        return postMultipartForJson("/v1/validate/zugferd", new Multipart().file("file", pdf), ValidationResult.class);
    }

    /**
     * Validates an XRechnung XML file (UBL or CII) against EN 16931 and the
     * German CIUS rules (BR-DE).
     *
     * @param xml the XML file
     * @return the result; check {@link ValidationResult#isValid()}
     */
    public ValidationResult validateXRechnung(Path xml) {
        return validateXRechnung(FileUpload.of(xml));
    }

    /**
     * Validates an XRechnung XML upload.
     *
     * @param xml the XML upload
     * @return the result; check {@link ValidationResult#isValid()}
     * @see #validateXRechnung(Path)
     */
    public ValidationResult validateXRechnung(FileUpload xml) {
        return postMultipartForJson("/v1/validate/xrechnung", new Multipart().file("file", xml), ValidationResult.class);
    }

    /**
     * Validates a ZUGFeRD PDF like {@link #validateZugferd(Path)} and returns a printable
     * PDF report with the verdict. An invalid invoice is a normal result, not an
     * exception, and so is a PDF without embedded XML, which gets a report saying so.
     *
     * @param pdf the PDF file
     * @return the report; check {@link ValidationReport#getValid()}
     */
    public ValidationReport validateZugferdReport(Path pdf) {
        return validateZugferdReport(FileUpload.of(pdf), null);
    }

    /**
     * Validates a ZUGFeRD PDF upload and returns a printable PDF report with the verdict.
     *
     * @param pdf the PDF upload
     * @return the report; check {@link ValidationReport#getValid()}
     * @see #validateZugferdReport(Path)
     */
    public ValidationReport validateZugferdReport(FileUpload pdf) {
        return validateZugferdReport(pdf, null);
    }

    /**
     * Validates a ZUGFeRD PDF upload and returns a printable PDF report with options.
     *
     * @param pdf the PDF upload
     * @param options the options, or {@code null} for the defaults
     * @return the report; check {@link ValidationReport#getValid()}
     * @see #validateZugferdReport(Path)
     */
    public ValidationReport validateZugferdReport(FileUpload pdf, ReportOptions options) {
        return postReport("/v1/validate/zugferd/report", pdf, options);
    }

    /**
     * Validates an XRechnung XML file (UBL or CII) like {@link #validateXRechnung(Path)}
     * and returns a printable PDF report with the verdict. An invalid invoice is a
     * normal result, not an exception.
     *
     * @param xml the XML file
     * @return the report; check {@link ValidationReport#getValid()}
     */
    public ValidationReport validateXRechnungReport(Path xml) {
        return validateXRechnungReport(FileUpload.of(xml), null);
    }

    /**
     * Validates an XRechnung XML upload and returns a printable PDF report with the verdict.
     *
     * @param xml the XML upload
     * @return the report; check {@link ValidationReport#getValid()}
     * @see #validateXRechnungReport(Path)
     */
    public ValidationReport validateXRechnungReport(FileUpload xml) {
        return validateXRechnungReport(xml, null);
    }

    /**
     * Validates an XRechnung XML upload and returns a printable PDF report with options.
     *
     * @param xml the XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the report; check {@link ValidationReport#getValid()}
     * @see #validateXRechnungReport(Path)
     */
    public ValidationReport validateXRechnungReport(FileUpload xml, ReportOptions options) {
        return postReport("/v1/validate/xrechnung/report", xml, options);
    }

    // ----------------------------------------------------------------- embed

    /**
     * Embeds a CII invoice XML into an existing PDF and returns a ZUGFeRD PDF/A-3.
     * The XML is validated with the full rule set first.
     *
     * @param pdf the visual PDF
     * @param xml the CII XML
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the XML violates the rule set
     */
    public byte[] embedZugferd(Path pdf, Path xml) {
        return embedZugferd(FileUpload.of(pdf), FileUpload.of(xml), null);
    }

    /**
     * Embeds a CII invoice XML into an existing PDF.
     *
     * @param pdf the visual PDF
     * @param xml the CII XML
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the XML violates the rule set
     * @see #embedZugferd(Path, Path)
     */
    public byte[] embedZugferd(FileUpload pdf, FileUpload xml) {
        return embedZugferd(pdf, xml, null);
    }

    /**
     * Embeds a CII invoice XML into an existing PDF with options.
     *
     * @param pdf the visual PDF
     * @param xml the CII XML
     * @param options the options, or {@code null} for the defaults
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the XML violates the rule set
     * @see #embedZugferd(Path, Path)
     */
    public byte[] embedZugferd(FileUpload pdf, FileUpload xml, EmbedOptions options) {
        Multipart form = new Multipart().file("pdf", pdf).file("xml", xml);
        if (options != null && options.isSkipValidation()) {
            form.field("skipValidation", "true");
        }
        return postMultipart("/v1/embed/zugferd", form, null).body();
    }

    // ----------------------------------------------------------------- email

    /**
     * Creates a ZUGFeRD hybrid invoice like {@link #createZugferd(InvoiceDocument)}
     * and sends it as a real email from your own verified domain. The result is
     * the message id of that email, not the file.
     *
     * <p>The sender must be on a domain added and verified on the Verified email
     * domains page of the dashboard. A result means the email was handed over for
     * delivery, not that the recipient's server has received it yet.
     *
     * <p>Only HTTP 429 is retried, because nothing was sent. After an
     * {@link InvoiceXmlConnectionException} the email may or may not have gone out,
     * so check before you send it again.
     *
     * @param invoice the invoice
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @throws InvoiceXmlApiException with {@link ErrorCodes#EMAIL_DOMAIN_NOT_VERIFIED},
     *         {@link ErrorCodes#EMAIL_NOT_IN_PLAN} or {@link ErrorCodes#EMAIL_SENDING_DISABLED}
     *         when the email cannot be sent
     */
    public EmailSendResult emailZugferd(InvoiceDocument invoice, EmailMessage email) {
        return emailZugferd(new CreateInvoiceRequest().setInvoice(invoice), email);
    }

    /**
     * Creates a ZUGFeRD hybrid invoice with rendering options and sends it as a real
     * email from your own verified domain. The result is the message id, not the file.
     *
     * @param invoice the invoice
     * @param email the sender, recipient and optional texts
     * @param options the options, or {@code null} for the defaults
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailZugferd(InvoiceDocument invoice, EmailMessage email, InvoiceOptions options) {
        return emailZugferd(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options), email);
    }

    /**
     * Creates a ZUGFeRD hybrid invoice from a full request envelope and sends it as a
     * real email from your own verified domain. The result is the message id, not the file.
     *
     * @param request the request
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailZugferd(CreateInvoiceRequest request, EmailMessage email) {
        return postEmail("/v1/create/zugferd/email", request, email);
    }

    /**
     * Creates an XRechnung like {@link #createXRechnung(InvoiceDocument)} and sends it
     * as a real email from your own verified domain. The result is the message id of
     * that email, not the file.
     *
     * @param invoice the invoice
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailXRechnung(InvoiceDocument invoice, EmailMessage email) {
        return emailXRechnung(new CreateInvoiceRequest().setInvoice(invoice), email);
    }

    /**
     * Creates an XRechnung with options and sends it as a real email from your own
     * verified domain. The result is the message id, not the file.
     *
     * @param invoice the invoice
     * @param email the sender, recipient and optional texts
     * @param options the options, or {@code null} for the defaults
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailXRechnung(InvoiceDocument invoice, EmailMessage email, InvoiceOptions options) {
        return emailXRechnung(new CreateInvoiceRequest().setInvoice(invoice).setOptions(options), email);
    }

    /**
     * Creates an XRechnung from a full request envelope and sends it as a real email
     * from your own verified domain. The result is the message id, not the file.
     *
     * @param request the request
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the invoice violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailXRechnung(CreateInvoiceRequest request, EmailMessage email) {
        return postEmail("/v1/create/xrechnung/email", request, email);
    }

    /**
     * Embeds a CII invoice XML into an existing PDF like
     * {@link #embedZugferd(Path, Path)} and sends the ZUGFeRD PDF/A-3 as a real email
     * from your own verified domain. The result is the message id, not the file. The
     * XML always runs through the full rule set first.
     *
     * @param pdf the visual PDF
     * @param xml the CII XML
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the XML violates the rule set or the API rejects an email field
     * @see #emailZugferd(InvoiceDocument, EmailMessage)
     */
    public EmailSendResult emailEmbeddedZugferd(Path pdf, Path xml, EmailMessage email) {
        return emailEmbeddedZugferd(FileUpload.of(pdf), FileUpload.of(xml), email);
    }

    /**
     * Embeds a CII invoice XML into an existing PDF and sends the ZUGFeRD PDF/A-3 as a
     * real email from your own verified domain. The result is the message id, not the file.
     *
     * @param pdf the visual PDF
     * @param xml the CII XML
     * @param email the sender, recipient and optional texts
     * @return the status and message id of the sent email
     * @throws InvoiceValidationException when the XML violates the rule set or the API rejects an email field
     * @see #emailEmbeddedZugferd(Path, Path, EmailMessage)
     */
    public EmailSendResult emailEmbeddedZugferd(FileUpload pdf, FileUpload xml, EmailMessage email) {
        Objects.requireNonNull(email, "email");
        Multipart form = new Multipart().file("pdf", pdf).file("xml", xml);
        addEmail(form, wireEmail(email));
        return postMultipartForJson("/v1/embed/zugferd/email", form, EmailSendResult.class);
    }

    // ---------------------------------------------------------------- render

    /**
     * Renders an invoice XML, such as an XRechnung or the CII XML of a ZUGFeRD
     * invoice, as a human-readable PDF. The format is detected from the XML:
     * CII, UBL and XRechnung are accepted.
     *
     * @param xml the invoice XML file
     * @return the PDF bytes
     */
    public byte[] renderXmlToPdf(Path xml) {
        return renderXmlToPdf(FileUpload.of(xml), null);
    }

    /**
     * Renders an invoice XML upload as a human-readable PDF.
     *
     * @param xml the invoice XML upload
     * @return the PDF bytes
     * @see #renderXmlToPdf(Path)
     */
    public byte[] renderXmlToPdf(FileUpload xml) {
        return renderXmlToPdf(xml, null);
    }

    /**
     * Renders an invoice XML upload as a human-readable PDF with options.
     *
     * @param xml the invoice XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the PDF bytes
     * @see #renderXmlToPdf(Path)
     */
    public byte[] renderXmlToPdf(FileUpload xml, RenderOptions options) {
        Multipart form = new Multipart().file("file", xml);
        if (options != null) {
            form.field("language", language(options.getLanguage()));
            form.field("logoUrl", options.getLogoUrl());
            form.field("includeQrCode", flag(options.getIncludeQrCode()));
        }
        return postMultipart("/v1/render/xml/to/pdf", form, null).body();
    }

    // --------------------------------------------------------------- convert

    /**
     * Converts a ZUGFeRD PDF to UBL 2.1 XML. The XML embedded in the PDF is read and
     * transcoded, and the result is validated before it is returned.
     *
     * @param pdf the ZUGFeRD PDF
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @throws InvoiceXmlApiException with {@link ErrorCodes#NO_EMBEDDED_XML} when the PDF carries no XML
     */
    public String convertZugferdToUbl(Path pdf) {
        return convertZugferdToUbl(FileUpload.of(pdf), null);
    }

    /**
     * Converts a ZUGFeRD PDF upload to UBL 2.1 XML.
     *
     * @param pdf the ZUGFeRD PDF upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertZugferdToUbl(Path)
     */
    public String convertZugferdToUbl(FileUpload pdf) {
        return convertZugferdToUbl(pdf, null);
    }

    /**
     * Converts a ZUGFeRD PDF upload to UBL 2.1 XML with options.
     *
     * @param pdf the ZUGFeRD PDF upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertZugferdToUbl(Path)
     */
    public String convertZugferdToUbl(FileUpload pdf, ConvertOptions options) {
        return convertToXml("/v1/convert/zugferd/to/ubl", pdf, options);
    }

    /**
     * Converts a ZUGFeRD PDF to CII XML. The XML embedded in the PDF is read and
     * rebuilt, and the result is validated before it is returned.
     *
     * @param pdf the ZUGFeRD PDF
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @throws InvoiceXmlApiException with {@link ErrorCodes#NO_EMBEDDED_XML} when the PDF carries no XML
     */
    public String convertZugferdToCii(Path pdf) {
        return convertZugferdToCii(FileUpload.of(pdf), null);
    }

    /**
     * Converts a ZUGFeRD PDF upload to CII XML.
     *
     * @param pdf the ZUGFeRD PDF upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertZugferdToCii(Path)
     */
    public String convertZugferdToCii(FileUpload pdf) {
        return convertZugferdToCii(pdf, null);
    }

    /**
     * Converts a ZUGFeRD PDF upload to CII XML with options.
     *
     * @param pdf the ZUGFeRD PDF upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertZugferdToCii(Path)
     */
    public String convertZugferdToCii(FileUpload pdf, ConvertOptions options) {
        return convertToXml("/v1/convert/zugferd/to/cii", pdf, options);
    }

    /**
     * Converts an XRechnung (UBL or CII) to plain EN 16931 UBL 2.1 XML. The result
     * declares EN 16931 instead of the XRechnung specification.
     *
     * @param xml the XRechnung XML
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public String convertXRechnungToUbl(Path xml) {
        return convertXRechnungToUbl(FileUpload.of(xml), null);
    }

    /**
     * Converts an XRechnung upload to plain EN 16931 UBL 2.1 XML.
     *
     * @param xml the XRechnung XML upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToUbl(Path)
     */
    public String convertXRechnungToUbl(FileUpload xml) {
        return convertXRechnungToUbl(xml, null);
    }

    /**
     * Converts an XRechnung upload to plain EN 16931 UBL 2.1 XML with options.
     *
     * @param xml the XRechnung XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToUbl(Path)
     */
    public String convertXRechnungToUbl(FileUpload xml, ConvertOptions options) {
        return convertToXml("/v1/convert/xrechnung/to/ubl", xml, options);
    }

    /**
     * Converts an XRechnung (UBL or CII) to plain EN 16931 CII XML. The result
     * declares EN 16931 instead of the XRechnung specification.
     *
     * @param xml the XRechnung XML
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public String convertXRechnungToCii(Path xml) {
        return convertXRechnungToCii(FileUpload.of(xml), null);
    }

    /**
     * Converts an XRechnung upload to plain EN 16931 CII XML.
     *
     * @param xml the XRechnung XML upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToCii(Path)
     */
    public String convertXRechnungToCii(FileUpload xml) {
        return convertXRechnungToCii(xml, null);
    }

    /**
     * Converts an XRechnung upload to plain EN 16931 CII XML with options.
     *
     * @param xml the XRechnung XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToCii(Path)
     */
    public String convertXRechnungToCii(FileUpload xml, ConvertOptions options) {
        return convertToXml("/v1/convert/xrechnung/to/cii", xml, options);
    }

    /**
     * Converts UBL 2.1 XML to an XRechnung in UBL syntax. The XRechnung rules apply,
     * so the source must carry what they require, such as the Leitweg-ID.
     *
     * @param xml the UBL XML
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public String convertUblToXRechnung(Path xml) {
        return convertUblToXRechnung(FileUpload.of(xml), null);
    }

    /**
     * Converts a UBL 2.1 XML upload to an XRechnung in UBL syntax.
     *
     * @param xml the UBL XML upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertUblToXRechnung(Path)
     */
    public String convertUblToXRechnung(FileUpload xml) {
        return convertUblToXRechnung(xml, null);
    }

    /**
     * Converts a UBL 2.1 XML upload to an XRechnung in UBL syntax with options.
     *
     * @param xml the UBL XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertUblToXRechnung(Path)
     */
    public String convertUblToXRechnung(FileUpload xml, ConvertOptions options) {
        return convertToXml("/v1/convert/ubl/to/xrechnung", xml, options);
    }

    /**
     * Converts CII XML to an XRechnung in UBL syntax. The XRechnung rules apply,
     * so the source must carry what they require, such as the Leitweg-ID.
     *
     * @param xml the CII XML
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public String convertCiiToXRechnung(Path xml) {
        return convertCiiToXRechnung(FileUpload.of(xml), null);
    }

    /**
     * Converts a CII XML upload to an XRechnung in UBL syntax.
     *
     * @param xml the CII XML upload
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertCiiToXRechnung(Path)
     */
    public String convertCiiToXRechnung(FileUpload xml) {
        return convertCiiToXRechnung(xml, null);
    }

    /**
     * Converts a CII XML upload to an XRechnung in UBL syntax with options.
     *
     * @param xml the CII XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertCiiToXRechnung(Path)
     */
    public String convertCiiToXRechnung(FileUpload xml, ConvertOptions options) {
        return convertToXml("/v1/convert/cii/to/xrechnung", xml, options);
    }

    /**
     * Converts CII XML to a ZUGFeRD PDF/A-3: a PDF is rendered from the invoice and
     * the XML embedded into it, after validation with the full rule set.
     *
     * @param xml the CII XML
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public byte[] convertCiiToZugferd(Path xml) {
        return convertCiiToZugferd(FileUpload.of(xml), null);
    }

    /**
     * Converts a CII XML upload to a ZUGFeRD PDF/A-3.
     *
     * @param xml the CII XML upload
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertCiiToZugferd(Path)
     */
    public byte[] convertCiiToZugferd(FileUpload xml) {
        return convertCiiToZugferd(xml, null);
    }

    /**
     * Converts a CII XML upload to a ZUGFeRD PDF/A-3 with options.
     *
     * @param xml the CII XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertCiiToZugferd(Path)
     */
    public byte[] convertCiiToZugferd(FileUpload xml, ConvertToPdfOptions options) {
        return convertToPdf("/v1/convert/cii/to/zugferd", xml, options);
    }

    /**
     * Converts UBL 2.1 XML to a ZUGFeRD PDF/A-3: a PDF is rendered from the invoice
     * and the invoice embedded into it as CII XML, after validation with the full rule set.
     *
     * @param xml the UBL XML
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public byte[] convertUblToZugferd(Path xml) {
        return convertUblToZugferd(FileUpload.of(xml), null);
    }

    /**
     * Converts a UBL 2.1 XML upload to a ZUGFeRD PDF/A-3.
     *
     * @param xml the UBL XML upload
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertUblToZugferd(Path)
     */
    public byte[] convertUblToZugferd(FileUpload xml) {
        return convertUblToZugferd(xml, null);
    }

    /**
     * Converts a UBL 2.1 XML upload to a ZUGFeRD PDF/A-3 with options.
     *
     * @param xml the UBL XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertUblToZugferd(Path)
     */
    public byte[] convertUblToZugferd(FileUpload xml, ConvertToPdfOptions options) {
        return convertToPdf("/v1/convert/ubl/to/zugferd", xml, options);
    }

    /**
     * Converts an XRechnung (UBL or CII) to a ZUGFeRD PDF/A-3: a PDF is rendered from
     * the invoice and the invoice embedded into it as CII XML, after validation with
     * the full rule set.
     *
     * @param xml the XRechnung XML
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public byte[] convertXRechnungToZugferd(Path xml) {
        return convertXRechnungToZugferd(FileUpload.of(xml), null);
    }

    /**
     * Converts an XRechnung upload to a ZUGFeRD PDF/A-3.
     *
     * @param xml the XRechnung XML upload
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToZugferd(Path)
     */
    public byte[] convertXRechnungToZugferd(FileUpload xml) {
        return convertXRechnungToZugferd(xml, null);
    }

    /**
     * Converts an XRechnung upload to a ZUGFeRD PDF/A-3 with options.
     *
     * @param xml the XRechnung XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #convertXRechnungToZugferd(Path)
     */
    public byte[] convertXRechnungToZugferd(FileUpload xml, ConvertToPdfOptions options) {
        return convertToPdf("/v1/convert/xrechnung/to/zugferd", xml, options);
    }

    // ------------------------------------------------------------- transform

    /**
     * Reads a PDF invoice and returns a ZUGFeRD PDF/A-3. AI reads the PDF, so a person
     * must check the result before it is used.
     *
     * <p>By default the PDF is rendered fresh from the data read and ends with an
     * Electronic invoice details section, so the technical values can be checked on
     * the pages.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF invoice
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public byte[] transformToZugferd(Path pdf) {
        return transformToZugferd(FileUpload.of(pdf), null);
    }

    /**
     * Reads a PDF invoice upload and returns a ZUGFeRD PDF/A-3. AI reads the PDF, so a
     * person must check the result before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF upload
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #transformToZugferd(Path)
     */
    public byte[] transformToZugferd(FileUpload pdf) {
        return transformToZugferd(pdf, null);
    }

    /**
     * Reads a PDF invoice upload and returns a ZUGFeRD PDF/A-3 with options. AI reads
     * the PDF, so a person must check the result before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF upload
     * @param options the options, or {@code null} for the defaults
     * @return the hybrid PDF bytes
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #transformToZugferd(Path)
     */
    public byte[] transformToZugferd(FileUpload pdf, TransformOptions options) {
        Multipart form = new Multipart().file("file", pdf);
        if (options != null) {
            form.field("language", language(options.getLanguage()));
            form.field("preserveOriginalPdf", flag(options.getPreserveOriginalPdf()));
            form.field("includeAdvancedProperties", flag(options.getIncludeAdvancedProperties()));
            form.field("validationLanguage", language(options.getValidationLanguage()));
        }
        return postMultipartOnce("/v1/transform/to/zugferd", form, null).body();
    }

    /**
     * Reads a PDF invoice and returns an XRechnung XML. AI reads the PDF, so a person
     * must check the result before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF invoice
     * @param buyerReference the buyer reference, for German public buyers the Leitweg-ID;
     *        it takes precedence over any reference on the PDF
     * @return the XML document
     * @throws IllegalArgumentException when the buyer reference is null, empty or blank
     * @throws InvoiceValidationException when the result violates the rule set
     */
    public String transformToXRechnung(Path pdf, String buyerReference) {
        return transformToXRechnung(FileUpload.of(pdf), buyerReference, null);
    }

    /**
     * Reads a PDF invoice upload and returns an XRechnung XML. AI reads the PDF, so a
     * person must check the result before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF upload
     * @param buyerReference the buyer reference, for German public buyers the Leitweg-ID
     * @return the XML document
     * @throws IllegalArgumentException when the buyer reference is null, empty or blank
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #transformToXRechnung(Path, String)
     */
    public String transformToXRechnung(FileUpload pdf, String buyerReference) {
        return transformToXRechnung(pdf, buyerReference, null);
    }

    /**
     * Reads a PDF invoice upload and returns an XRechnung XML with options. AI reads
     * the PDF, so a person must check the result before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF upload
     * @param buyerReference the buyer reference, for German public buyers the Leitweg-ID
     * @param options the options, or {@code null} for the defaults
     * @return the XML document
     * @throws IllegalArgumentException when the buyer reference is null, empty or blank
     * @throws InvoiceValidationException when the result violates the rule set
     * @see #transformToXRechnung(Path, String)
     */
    public String transformToXRechnung(FileUpload pdf, String buyerReference, TransformToXRechnungOptions options) {
        if (buyerReference == null || buyerReference.isBlank()) {
            throw new IllegalArgumentException("buyerReference must not be empty");
        }
        Multipart form = new Multipart().file("file", pdf).field("buyerReference", buyerReference);
        if (options != null) {
            form.field("validationLanguage", language(options.getValidationLanguage()));
        }
        return new String(postMultipartOnce("/v1/transform/to/xrechnung", form, null).body(), StandardCharsets.UTF_8);
    }

    // ----------------------------------------------------------------- parse

    /**
     * Reads a PDF invoice, typed, scanned or photographed, into the structured invoice
     * model. AI reads the PDF, so a person must check the data before it is used. XML
     * embedded in the PDF is ignored; use {@link #extractJson(Path)} to read it.
     *
     * <p>The result carries a confidence score per area from 0.0 (had to guess) to 1.0
     * (unambiguous), and the language the invoice is written in as an ISO 639-1 code.
     * Both can be missing.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF invoice
     * @return the invoice with its confidence and language
     */
    public ParsedInvoice parseJson(Path pdf) {
        return parseJson(FileUpload.of(pdf));
    }

    /**
     * Reads a PDF invoice upload into the structured invoice model. AI reads the PDF,
     * so a person must check the data before it is used.
     *
     * <p>A 429 on this method is not retried automatically, so wait for
     * {@code Retry-After} before calling again if you want to retry.
     *
     * @param pdf the PDF upload
     * @return the invoice with its confidence and language
     * @see #parseJson(Path)
     */
    public ParsedInvoice parseJson(FileUpload pdf) {
        return postMultipartOnceForJson("/v1/parse/json", new Multipart().file("file", pdf), ParsedInvoice.class);
    }

    // --------------------------------------------------------------- extract

    /**
     * Reads a ZUGFeRD PDF or an XRechnung XML into the structured invoice model.
     *
     * @param file the PDF or XML file
     * @return the invoice
     */
    public InvoiceDocument extractJson(Path file) {
        return extractJson(FileUpload.of(file));
    }

    /**
     * Reads a ZUGFeRD PDF or an XRechnung XML upload into the structured invoice model.
     *
     * @param file the PDF or XML upload
     * @return the invoice
     * @see #extractJson(Path)
     */
    public InvoiceDocument extractJson(FileUpload file) {
        ExtractJsonResponse response = postMultipartForJson("/v1/extract/json",
                new Multipart().file("file", file), ExtractJsonResponse.class);
        return response.invoice;
    }

    /**
     * Extracts the supporting documents embedded in an invoice, such as
     * delivery notes or timesheets, as a ZIP archive. Payloads are decoded and
     * returned verbatim. External URIs are not fetched.
     *
     * @param file the PDF or XML file
     * @return the ZIP bytes
     * @throws InvoiceXmlApiException with {@link ErrorCodes#NO_ATTACHMENTS} when the invoice carries none
     */
    public byte[] extractAttachments(Path file) {
        return extractAttachments(FileUpload.of(file));
    }

    /**
     * Extracts the supporting documents embedded in an invoice upload as a ZIP archive.
     *
     * @param file the PDF or XML upload
     * @return the ZIP bytes
     * @throws InvoiceXmlApiException with {@link ErrorCodes#NO_ATTACHMENTS} when the invoice carries none
     * @see #extractAttachments(Path)
     */
    public byte[] extractAttachments(FileUpload file) {
        return postMultipart("/v1/extract/attachments", new Multipart().file("file", file), null).body();
    }

    // -------------------------------------------------------------- plumbing

    private String convertToXml(String path, FileUpload file, ConvertOptions options) {
        Multipart form = new Multipart().file("file", file);
        if (options != null) {
            form.field("validationLanguage", language(options.getValidationLanguage()));
        }
        return new String(postMultipart(path, form, null).body(), StandardCharsets.UTF_8);
    }

    private byte[] convertToPdf(String path, FileUpload xml, ConvertToPdfOptions options) {
        Multipart form = new Multipart().file("file", xml);
        if (options != null) {
            form.field("language", language(options.getLanguage()));
            form.field("validationLanguage", language(options.getValidationLanguage()));
        }
        return postMultipart(path, form, null).body();
    }

    private ValidationReport postReport(String path, FileUpload file, ReportOptions options) {
        Multipart form = new Multipart().file("file", file);
        if (options != null) {
            form.field("validationLanguage", language(options.getValidationLanguage()));
        }
        TransportResponse response = postMultipart(path, form, null);
        return new ValidationReport(response.body(), verdict(response.header("X-Invoice-Valid")));
    }

    /** The verdict header's value, {@code null} when it is missing or says neither. */
    private static Boolean verdict(String header) {
        if ("true".equalsIgnoreCase(header)) {
            return Boolean.TRUE;
        }
        return "false".equalsIgnoreCase(header) ? Boolean.FALSE : null;
    }

    /** The form value of a language, {@code null} (not sent) when unset. */
    private static String language(PdfLanguage language) {
        return language == null ? null : language.name().toLowerCase(Locale.ROOT);
    }

    /** The form value of a switch, {@code null} (not sent) when unset. */
    private static String flag(Boolean value) {
        return value == null ? null : value.toString();
    }

    /** The email as sent on every route: blank copy addresses dropped, the others trimmed. */
    private static EmailMessage wireEmail(EmailMessage email) {
        return new EmailMessage()
                .setFrom(email.getFrom())
                .setFromName(email.getFromName())
                .setTo(email.getTo())
                .setCc(copyAddresses(email.getCc()))
                .setBcc(copyAddresses(email.getBcc()))
                .setReplyTo(email.getReplyTo())
                .setSubject(email.getSubject())
                .setMessage(email.getMessage());
    }

    private static List<String> copyAddresses(List<String> addresses) {
        if (addresses == null) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String address : addresses) {
            if (address != null && !address.isBlank()) {
                kept.add(address.strip());
            }
        }
        return kept.isEmpty() ? null : kept;
    }

    private static void addEmail(Multipart form, EmailMessage email) {
        form.field("email.from", email.getFrom());
        form.field("email.fromName", email.getFromName());
        form.field("email.to", email.getTo());
        addEach(form, "email.cc", email.getCc());
        addEach(form, "email.bcc", email.getBcc());
        form.field("email.replyTo", email.getReplyTo());
        form.field("email.subject", email.getSubject());
        form.field("email.message", email.getMessage());
    }

    private static void addEach(Multipart form, String name, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            form.field(name, value);
        }
    }

    private EmailSendResult postEmail(String path, CreateInvoiceRequest request, EmailMessage email) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(email, "email");
        return readJson(path, postJson(path, new EmailInvoiceRequest(request, wireEmail(email)), "application/json"),
                EmailSendResult.class);
    }

    private TransportResponse postJson(String path, Object body, String accept) {
        Objects.requireNonNull(body, "request");
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new InvoiceXmlException("Cannot serialize the request: " + e.getOriginalMessage(), e);
        }
        return execute(path, "application/json", json, accept);
    }

    private TransportResponse postMultipart(String path, Multipart form, String accept) {
        return execute(path, form.contentType(), form.build(), accept);
    }

    private TransportResponse postMultipartOnce(String path, Multipart form, String accept) {
        return execute(path, form.contentType(), form.build(), accept, 0);
    }

    private <T> T postMultipartForJson(String path, Multipart form, Class<T> type) {
        return readJson(path, postMultipart(path, form, "application/json"), type);
    }

    private <T> T postMultipartOnceForJson(String path, Multipart form, Class<T> type) {
        return readJson(path, postMultipartOnce(path, form, "application/json"), type);
    }

    private <T> T readJson(String path, TransportResponse response, Class<T> type) {
        try {
            return mapper.readValue(response.body(), type);
        } catch (IOException e) {
            throw new InvoiceXmlException("Cannot read the response of " + path + ": " + e.getMessage(), e);
        }
    }

    private TransportResponse execute(String path, String contentType, byte[] body, String accept) {
        return execute(path, contentType, body, accept, maxRetries);
    }

    private TransportResponse execute(String path, String contentType, byte[] body, String accept, int retries) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", contentType);
        if (accept != null) {
            headers.put("Accept", accept);
        }
        headers.put("User-Agent", userAgent);
        TransportRequest request = new TransportRequest("POST", URI.create(baseUrl + path), headers, body, timeout);

        for (int attempt = 0; ; attempt++) {
            TransportResponse response;
            try {
                response = transport.send(request);
            } catch (IOException e) {
                throw new InvoiceXmlConnectionException("Request to " + path + " failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InvoiceXmlConnectionException("Interrupted while calling " + path, e);
            }
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response;
            }
            if (status == 429 && attempt < retries) {
                Duration wait = RetryAfter.parse(response.header("Retry-After"), clock);
                if (wait == null) {
                    wait = Duration.ofSeconds(1L << Math.min(attempt, 5));
                }
                if (wait.compareTo(maxRetryWait) <= 0) {
                    try {
                        sleeper.sleep(wait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InvoiceXmlConnectionException("Interrupted while waiting to retry " + path, e);
                    }
                    continue;
                }
            }
            throw ErrorMapper.toException(response, mapper, clock);
        }
    }

    private static String loadVersion() {
        try (InputStream in = ZugferdXRechnungClient.class.getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("version");
                if (v != null && !v.startsWith("$")) {
                    return v;
                }
            }
        } catch (IOException ignored) {
            // fall through
        }
        return "dev";
    }

    /** Response envelope of {@code /v1/extract/json}. */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ExtractJsonResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("invoice")
        InvoiceDocument invoice;
    }

    /** Request body of the create email endpoints: the create envelope plus {@code email}. */
    private static final class EmailInvoiceRequest {
        @com.fasterxml.jackson.annotation.JsonUnwrapped
        final CreateInvoiceRequest request;

        @com.fasterxml.jackson.annotation.JsonProperty("email")
        final EmailMessage email;

        EmailInvoiceRequest(CreateInvoiceRequest request, EmailMessage email) {
            this.request = request;
            this.email = email;
        }
    }

    /**
     * Builder for {@link ZugferdXRechnungClient}.
     */
    public static final class Builder {

        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private HttpTransport transport;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration timeout = Duration.ofSeconds(120);
        private int maxRetries = 3;
        private Duration maxRetryWait = Duration.ofSeconds(60);
        private Sleeper sleeper = Sleeper.SYSTEM;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Sets the API key, sent as {@code Authorization: Bearer <key>}. Required.
         *
         * @param apiKey the API key
         * @return this builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the API base URL. Defaults to {@value ZugferdXRechnungClient#DEFAULT_BASE_URL}.
         *
         * @param baseUrl the base URL, without the {@code /v1} path
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the TCP connect timeout of the default transport. Defaults to 10 seconds.
         * Ignored when a custom {@link #transport(HttpTransport)} is set.
         *
         * @param connectTimeout the timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the overall timeout of one request. Defaults to 120 seconds.
         *
         * @param timeout the timeout
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets how many times a request answered with HTTP 429 is retried after the
         * {@code Retry-After} delay, on every call except transform and parse. Defaults to 3.
         * Use 0 to disable retries.
         *
         * @param maxRetries the number of retries
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the longest {@code Retry-After} delay the client waits out by itself.
         * A longer delay raises a {@link RateLimitException} immediately. Defaults to 60 seconds.
         *
         * @param maxRetryWait the longest delay
         * @return this builder
         */
        public Builder maxRetryWait(Duration maxRetryWait) {
            this.maxRetryWait = maxRetryWait;
            return this;
        }

        /**
         * Replaces the HTTP layer, for example to reuse a configured
         * {@link java.net.http.HttpClient} via {@link JdkHttpTransport#JdkHttpTransport(java.net.http.HttpClient)}.
         *
         * @param transport the transport
         * @return this builder
         */
        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        Builder sleeper(Sleeper sleeper) {
            this.sleeper = sleeper;
            return this;
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Builds the client.
         *
         * @return the client
         * @throws IllegalArgumentException when the API key is missing or a setting is invalid
         */
        public ZugferdXRechnungClient build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("An API key is required. Get one at https://www.invoicexml.com/account/signup");
            }
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl must not be empty");
            }
            baseUrl = baseUrl.trim();
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            Objects.requireNonNull(connectTimeout, "connectTimeout");
            Objects.requireNonNull(timeout, "timeout");
            Objects.requireNonNull(maxRetryWait, "maxRetryWait");
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must not be negative");
            }
            return new ZugferdXRechnungClient(this);
        }
    }
}
