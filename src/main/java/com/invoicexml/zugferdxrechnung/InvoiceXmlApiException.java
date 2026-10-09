package com.invoicexml.zugferdxrechnung;

/**
 * Thrown when the API answers with an error status. Carries the RFC 7807
 * problem details the API returns, including the machine-readable
 * {@linkplain #getErrorCode() error code} (see {@link ErrorCodes}).
 *
 * <p>Specific statuses have their own subclasses:
 * {@link InvoiceValidationException} (400 with findings),
 * {@link AuthenticationException} (401),
 * {@link PayloadTooLargeException} (413) and
 * {@link RateLimitException} (429).
 */
public class InvoiceXmlApiException extends InvoiceXmlException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final Integer errorCode;
    private final String title;
    private final String detail;
    private final String traceId;
    private final String responseBody;

    /**
     * Creates an exception from a parsed problem response.
     *
     * @param statusCode the HTTP status code
     * @param errorCode the API error code, or {@code null}
     * @param title the problem title, or {@code null}
     * @param detail the problem detail, or {@code null}
     * @param traceId the trace identifier, or {@code null}
     * @param responseBody the raw response body
     */
    public InvoiceXmlApiException(int statusCode, Integer errorCode, String title, String detail,
            String traceId, String responseBody) {
        super(buildMessage(statusCode, errorCode, title, detail), null);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.title = title;
        this.detail = detail;
        this.traceId = traceId;
        this.responseBody = responseBody;
    }

    private static String buildMessage(int statusCode, Integer errorCode, String title, String detail) {
        StringBuilder sb = new StringBuilder("HTTP ").append(statusCode);
        if (errorCode != null) {
            sb.append(" (error ").append(errorCode).append(')');
        }
        if (title != null && !title.isEmpty()) {
            sb.append(": ").append(title);
        }
        if (detail != null && !detail.isEmpty()) {
            sb.append(". ").append(detail);
        }
        return sb.toString();
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the API error code, for example {@link ErrorCodes#MODEL_VALIDATION}.
     *
     * @return the error code, or {@code null} when the response carried none
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the short problem title.
     *
     * @return the title, or {@code null}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the human-readable problem detail.
     *
     * @return the detail, or {@code null}
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns the trace identifier. Quote it when contacting support.
     *
     * @return the trace identifier, or {@code null}
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Returns the raw response body.
     *
     * @return the body text, possibly empty
     */
    public String getResponseBody() {
        return responseBody;
    }
}
