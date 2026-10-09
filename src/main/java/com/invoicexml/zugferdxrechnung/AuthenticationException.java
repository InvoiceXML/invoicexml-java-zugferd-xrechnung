package com.invoicexml.zugferdxrechnung;

/**
 * Thrown on HTTP 401: the API key is missing, unknown or inactive.
 */
public class AuthenticationException extends InvoiceXmlApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param errorCode the API error code, or {@code null}
     * @param title the problem title, or {@code null}
     * @param detail the problem detail, or {@code null}
     * @param traceId the trace identifier, or {@code null}
     * @param responseBody the raw response body
     */
    public AuthenticationException(Integer errorCode, String title, String detail, String traceId,
            String responseBody) {
        super(401, errorCode, title, detail, traceId, responseBody);
    }
}
