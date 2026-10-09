package com.invoicexml.zugferdxrechnung.transport;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An HTTP request as built by the client: method, absolute URI, headers, body and timeout.
 */
public final class TransportRequest {

    private final String method;
    private final URI uri;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Duration timeout;

    /**
     * Creates a request.
     *
     * @param method the HTTP method, for example {@code POST}
     * @param uri the absolute request URI
     * @param headers the request headers
     * @param body the request body, empty for none
     * @param timeout the overall request timeout
     */
    public TransportRequest(String method, URI uri, Map<String, String> headers, byte[] body, Duration timeout) {
        this.method = Objects.requireNonNull(method, "method");
        this.uri = Objects.requireNonNull(uri, "uri");
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        this.body = body == null ? new byte[0] : body;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    /**
     * Returns the HTTP method.
     *
     * @return the method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the absolute request URI.
     *
     * @return the URI
     */
    public URI uri() {
        return uri;
    }

    /**
     * Returns the request headers.
     *
     * @return an unmodifiable header map
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Returns the value of one header, matching the name case-insensitively.
     *
     * @param name the header name
     * @return the value, or {@code null} when absent
     */
    public String header(String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Returns the request body. The array is shared, do not modify it.
     *
     * @return the body bytes
     */
    public byte[] body() {
        return body;
    }

    /**
     * Returns the overall request timeout.
     *
     * @return the timeout
     */
    public Duration timeout() {
        return timeout;
    }
}
