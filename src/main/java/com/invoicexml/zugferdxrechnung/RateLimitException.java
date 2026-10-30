package com.invoicexml.zugferdxrechnung;

import java.time.Duration;

/**
 * Thrown on HTTP 429 once the client's automatic retries are used up, or on the
 * first 429 of a transform call: too many requests for this API key are being
 * processed at the same time.
 *
 * <p>Every call except transform already waits for the {@code Retry-After} delay
 * and retries (see {@link ZugferdXRechnungClient.Builder#maxRetries(int)}), so
 * seeing this exception there means the account stayed saturated. A 429 is not billed.
 */
public class RateLimitException extends InvoiceXmlApiException {

    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    /**
     * Creates an exception.
     *
     * @param errorCode the API error code, or {@code null}
     * @param title the problem title, or {@code null}
     * @param detail the problem detail, or {@code null}
     * @param traceId the trace identifier, or {@code null}
     * @param responseBody the raw response body
     * @param retryAfter the delay the API asked for, or {@code null} when none was sent
     */
    public RateLimitException(Integer errorCode, String title, String detail, String traceId,
            String responseBody, Duration retryAfter) {
        super(429, errorCode, title, detail, traceId, responseBody);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the delay the API asked the caller to wait before retrying.
     *
     * @return the delay, or {@code null} when the response had no usable {@code Retry-After} header
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
