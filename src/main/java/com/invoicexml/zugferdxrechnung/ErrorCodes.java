package com.invoicexml.zugferdxrechnung;

/**
 * Machine-readable error codes carried as {@code errorCode} in every problem
 * response, available through {@link InvoiceXmlApiException#getErrorCode()}.
 * Branch on these instead of parsing titles or messages.
 */
public final class ErrorCodes {

    /** An unexpected error. */
    public static final int UNKNOWN = 4000;
    /** The XML is not well-formed or failed schema or business rule validation. */
    public static final int XML_VALIDATION = 4001;
    /** The request body is missing required fields or contains invalid values. */
    public static final int MODEL_VALIDATION = 4002;
    /** No file was uploaded. */
    public static final int MISSING_FILE = 4003;
    /** The uploaded file has the wrong type for this endpoint. */
    public static final int UNSUPPORTED_CONTENT_TYPE = 4004;
    /** The uploaded file exceeds the size limit. */
    public static final int FILE_TOO_LARGE = 4005;
    /** The uploaded PDF carries no embedded invoice XML. */
    public static final int NO_EMBEDDED_XML = 4006;
    /** The PDF could not be processed. */
    public static final int PDF_ERROR = 4007;
    /** The document is not an invoice. */
    public static final int NOT_AN_INVOICE = 4008;
    /** The document contains more than one invoice. */
    public static final int MULTIPLE_INVOICES = 4009;
    /** The API key is missing, unknown or inactive. */
    public static final int UNAUTHORIZED = 4010;
    /** A URL given in the options (such as {@code pdfUrl} or {@code logoUrl}) could not be fetched. */
    public static final int REMOTE_FETCH_ERROR = 4011;
    /** The invoice carries no embedded attachments to extract. */
    public static final int NO_ATTACHMENTS = 4013;
    /** An embedded attachment could not be decoded. */
    public static final int INVALID_ATTACHMENT = 4014;
    /** The supplied PDF uses a font that cannot be embedded, so it cannot become PDF/A-3. */
    public static final int UNEMBEDDABLE_FONT = 4015;
    /** The host of a PDF or logo URL refused access (HTTP 403). */
    public static final int REMOTE_FETCH_FORBIDDEN = 4016;
    /** The XML declares a specification identifier that is not an official ZUGFeRD or Factur-X profile. */
    public static final int UNSUPPORTED_ZUGFERD_PROFILE = 4017;
    /** Too many concurrent requests for this API key. Retryable after the {@code Retry-After} delay. */
    public static final int TOO_MANY_REQUESTS = 4019;
    /** Your plan does not include sending invoices by email. Not billed. */
    public static final int EMAIL_NOT_IN_PLAN = 4020;
    /** Email sending is disabled or paused for this account. Not billed. */
    public static final int EMAIL_SENDING_DISABLED = 4021;
    /** The sender's domain is not verified yet. Not billed. */
    public static final int EMAIL_DOMAIN_NOT_VERIFIED = 4022;
    /** The request exceeded the processing time limit (HTTP 503). */
    public static final int PROCESSING_TIMEOUT = 4023;
    /** The amounts read from the PDF do not add up to its printed total. Billed; correct invoiceData and create the invoice from it. */
    public static final int TOTALS_MISMATCH = 4024;

    private ErrorCodes() {
    }
}
