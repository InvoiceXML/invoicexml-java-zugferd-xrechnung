package com.invoicexml.zugferdxrechnung;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoicexml.zugferdxrechnung.model.CreateInvoiceRequest;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceOptions;
import com.invoicexml.zugferdxrechnung.transport.HttpTransport;
import com.invoicexml.zugferdxrechnung.transport.JdkHttpTransport;
import com.invoicexml.zugferdxrechnung.transport.TransportRequest;
import com.invoicexml.zugferdxrechnung.transport.TransportResponse;
import com.invoicexml.zugferdxrechnung.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
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
 * delay. Other errors raise an {@link InvoiceXmlApiException} subclass, and
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

    // ---------------------------------------------------------------- render

    /**
     * Renders an XRechnung XML (UBL or CII) as a human-readable PDF.
     *
     * @param xml the XML file
     * @return the PDF bytes
     */
    public byte[] renderXRechnungToPdf(Path xml) {
        return renderXRechnungToPdf(FileUpload.of(xml), null);
    }

    /**
     * Renders an XRechnung XML upload as a human-readable PDF.
     *
     * @param xml the XML upload
     * @return the PDF bytes
     * @see #renderXRechnungToPdf(Path)
     */
    public byte[] renderXRechnungToPdf(FileUpload xml) {
        return renderXRechnungToPdf(xml, null);
    }

    /**
     * Renders an XRechnung XML upload as a human-readable PDF with options.
     *
     * @param xml the XML upload
     * @param options the options, or {@code null} for the defaults
     * @return the PDF bytes
     * @see #renderXRechnungToPdf(Path)
     */
    public byte[] renderXRechnungToPdf(FileUpload xml, RenderOptions options) {
        Multipart form = new Multipart().file("file", xml);
        if (options != null) {
            if (options.getLanguage() != null) {
                form.field("language", options.getLanguage().name().toLowerCase(Locale.ROOT));
            }
            form.field("logoUrl", options.getLogoUrl());
        }
        return postMultipart("/v1/render/xrechnung/to/pdf", form, null).body();
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

    private <T> T postMultipartForJson(String path, Multipart form, Class<T> type) {
        TransportResponse response = postMultipart(path, form, "application/json");
        try {
            return mapper.readValue(response.body(), type);
        } catch (IOException e) {
            throw new InvoiceXmlException("Cannot read the response of " + path + ": " + e.getMessage(), e);
        }
    }

    private TransportResponse execute(String path, String contentType, byte[] body, String accept) {
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
            if (status == 429 && attempt < maxRetries) {
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
         * {@code Retry-After} delay. Defaults to 3. Use 0 to disable retries.
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
                throw new IllegalArgumentException("An API key is required. Get one at https://invoicexml.com/account/signup");
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
