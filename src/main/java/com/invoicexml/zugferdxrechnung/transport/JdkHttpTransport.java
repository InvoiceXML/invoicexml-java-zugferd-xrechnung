package com.invoicexml.zugferdxrechnung.transport;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * {@link HttpTransport} backed by the JDK's {@link java.net.http.HttpClient}.
 */
public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient httpClient;

    /**
     * Creates a transport with a new {@link HttpClient} using the given connect timeout.
     *
     * @param connectTimeout the TCP connect timeout
     */
    public JdkHttpTransport(Duration connectTimeout) {
        this(HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout"))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /**
     * Creates a transport on top of an existing {@link HttpClient}, for example one
     * configured with a proxy, an executor or a custom SSL context.
     *
     * @param httpClient the client to send requests with
     */
    public JdkHttpTransport(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public TransportResponse send(TransportRequest request) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(request.timeout())
                .method(request.method(), HttpRequest.BodyPublishers.ofByteArray(request.body()));
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new TransportResponse(response.statusCode(), response.headers().map(), response.body());
    }
}
