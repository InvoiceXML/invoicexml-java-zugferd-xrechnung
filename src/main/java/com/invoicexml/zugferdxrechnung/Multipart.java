package com.invoicexml.zugferdxrechnung;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Builds a {@code multipart/form-data} body in memory. */
final class Multipart {

    private final String boundary = "invoicexml-" + UUID.randomUUID();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    Multipart file(String name, FileUpload file) {
        writeAscii("--" + boundary + "\r\n");
        writeUtf8("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + escape(file.getFileName()) + "\"\r\n");
        writeAscii("Content-Type: " + file.getContentType() + "\r\n\r\n");
        out.write(file.getContent(), 0, file.getContent().length);
        writeAscii("\r\n");
        return this;
    }

    Multipart field(String name, String value) {
        if (value == null) {
            return this;
        }
        writeAscii("--" + boundary + "\r\n");
        writeAscii("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeUtf8(value);
        writeAscii("\r\n");
        return this;
    }

    String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    byte[] build() {
        writeAscii("--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static String escape(String fileName) {
        return fileName.replace("\r", "").replace("\n", "").replace("\"", "%22");
    }

    private void writeAscii(String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
    }

    private void writeUtf8(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b, 0, b.length);
    }
}
