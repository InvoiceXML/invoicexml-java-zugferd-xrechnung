package com.invoicexml.zugferdxrechnung;

/**
 * Thrown on HTTP 413: an uploaded file exceeds the API's size limit (20 MB per file).
 */
public class PayloadTooLargeException extends InvoiceXmlApiException {

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
    public PayloadTooLargeException(Integer errorCode, String title, String detail, String traceId,
            String responseBody) {
        super(413, errorCode, title, detail, traceId, responseBody);
    }
}
