package com.invoicexml.zugferdxrechnung;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * A file to upload: name, content and media type.
 *
 * <p>The API recognises PDFs and XML files by media type or file extension, so
 * pick a factory that sets one of them correctly:
 * <pre>{@code
 * FileUpload.of(Path.of("invoice.pdf"));       // type from the extension
 * FileUpload.ofPdf(pdfBytes);                  // "invoice.pdf", application/pdf
 * FileUpload.ofXml(xmlString);                 // "invoice.xml", application/xml
 * }</pre>
 * Files up to 20 MB are accepted.
 */
public final class FileUpload {

    /** Media type of PDF uploads. */
    public static final String APPLICATION_PDF = "application/pdf";
    /** Media type of XML uploads. */
    public static final String APPLICATION_XML = "application/xml";
    /** Media type used when the type cannot be inferred. */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private final String fileName;
    private final byte[] content;
    private final String contentType;

    private FileUpload(String fileName, byte[] content, String contentType) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.content = Objects.requireNonNull(content, "content");
        this.contentType = Objects.requireNonNull(contentType, "contentType");
    }

    /**
     * Reads a file from disk. The media type is inferred from the extension.
     *
     * @param path the file to read
     * @return the upload
     * @throws UncheckedIOException when the file cannot be read
     */
    public static FileUpload of(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            Path name = path.getFileName();
            return of(name == null ? "upload" : name.toString(), Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    /**
     * Creates an upload from bytes. The media type is inferred from the file name's extension.
     *
     * @param fileName the file name sent to the API, for example {@code "invoice.pdf"}
     * @param content the file content; the array is not copied
     * @return the upload
     */
    public static FileUpload of(String fileName, byte[] content) {
        return new FileUpload(fileName, content, inferContentType(fileName));
    }

    /**
     * Creates an upload with an explicit media type.
     *
     * @param fileName the file name sent to the API
     * @param content the file content; the array is not copied
     * @param contentType the media type, for example {@value #APPLICATION_PDF}
     * @return the upload
     */
    public static FileUpload of(String fileName, byte[] content, String contentType) {
        return new FileUpload(fileName, content, contentType);
    }

    /**
     * Creates a PDF upload named {@code invoice.pdf}.
     *
     * @param pdf the PDF bytes; the array is not copied
     * @return the upload
     */
    public static FileUpload ofPdf(byte[] pdf) {
        return new FileUpload("invoice.pdf", pdf, APPLICATION_PDF);
    }

    /**
     * Creates an XML upload named {@code invoice.xml}.
     *
     * @param xml the XML bytes; the array is not copied
     * @return the upload
     */
    public static FileUpload ofXml(byte[] xml) {
        return new FileUpload("invoice.xml", xml, APPLICATION_XML);
    }

    /**
     * Creates an XML upload named {@code invoice.xml}, encoded as UTF-8.
     *
     * @param xml the XML text
     * @return the upload
     */
    public static FileUpload ofXml(String xml) {
        return ofXml(Objects.requireNonNull(xml, "xml").getBytes(StandardCharsets.UTF_8));
    }

    private static String inferContentType(String fileName) {
        String lower = Objects.requireNonNull(fileName, "fileName").toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return APPLICATION_PDF;
        }
        if (lower.endsWith(".xml")) {
            return APPLICATION_XML;
        }
        return APPLICATION_OCTET_STREAM;
    }

    /**
     * Returns the file name sent to the API.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file content. The array is shared, do not modify it.
     *
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Returns the media type sent with the file.
     *
     * @return the media type
     */
    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return "FileUpload{" + fileName + ", " + contentType + ", " + content.length + " bytes}";
    }
}
