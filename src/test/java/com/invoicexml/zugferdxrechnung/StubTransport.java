package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.transport.HttpTransport;
import com.invoicexml.zugferdxrechnung.transport.TransportRequest;
import com.invoicexml.zugferdxrechnung.transport.TransportResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Records requests and replays queued responses. */
final class StubTransport implements HttpTransport {

    final List<TransportRequest> requests = new ArrayList<>();
    private final Deque<Object> responses = new ArrayDeque<>();

    StubTransport respond(int status, String contentType, byte[] body, String... headerPairs) {
        Map<String, List<String>> headers = new HashMap<>();
        if (contentType != null) {
            headers.put("Content-Type", Collections.singletonList(contentType));
        }
        for (int i = 0; i + 1 < headerPairs.length; i += 2) {
            headers.put(headerPairs[i], Collections.singletonList(headerPairs[i + 1]));
        }
        responses.add(new TransportResponse(status, headers, body));
        return this;
    }

    StubTransport respondJson(int status, String json, String... headerPairs) {
        String type = status >= 400 ? "application/problem+json" : "application/json";
        return respond(status, type, json.getBytes(StandardCharsets.UTF_8), headerPairs);
    }

    StubTransport fail(IOException e) {
        responses.add(e);
        return this;
    }

    TransportRequest last() {
        return requests.get(requests.size() - 1);
    }

    @Override
    public TransportResponse send(TransportRequest request) throws IOException {
        requests.add(request);
        Object next = responses.poll();
        if (next == null) {
            throw new AssertionError("No stubbed response left for " + request.uri());
        }
        if (next instanceof IOException) {
            throw (IOException) next;
        }
        return (TransportResponse) next;
    }
}
