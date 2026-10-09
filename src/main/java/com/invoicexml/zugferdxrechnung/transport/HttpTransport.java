package com.invoicexml.zugferdxrechnung.transport;

import java.io.IOException;

/**
 * Sends one HTTP request and returns the raw response.
 *
 * <p>The client builds the complete request (URL, headers, body) and handles
 * retries and error mapping itself, so an implementation only moves bytes. The
 * default is {@link JdkHttpTransport}. Supply your own to route calls through a
 * proxy library, add tracing, or stub the API in tests.
 *
 * <p>Implementations must be thread-safe.
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * Sends the request.
     *
     * @param request the request to send
     * @return the response, whatever its status code
     * @throws IOException when the request could not be sent or the response not read
     * @throws InterruptedException when the calling thread was interrupted while waiting
     */
    TransportResponse send(TransportRequest request) throws IOException, InterruptedException;
}
