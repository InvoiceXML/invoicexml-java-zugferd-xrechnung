package com.invoicexml.zugferdxrechnung.validation;

import java.util.Objects;

/**
 * Result of a report call: the printable PDF and the verdict. An invalid invoice
 * is a normal result, not an exception.
 */
public final class ValidationReport {

    private final byte[] file;
    private final Boolean valid;

    /**
     * Creates a report result.
     *
     * @param file the report PDF; the array is not copied
     * @param valid the verdict, or {@code null} when the response carried none
     */
    public ValidationReport(byte[] file, Boolean valid) {
        this.file = Objects.requireNonNull(file, "file");
        this.valid = valid;
    }

    /**
     * Returns the report PDF. The array is shared, do not modify it.
     *
     * @return the PDF bytes
     */
    public byte[] getFile() {
        return file;
    }

    /**
     * Returns the verdict from the {@code X-Invoice-Valid} response header.
     *
     * @return {@code true} or {@code false}, or {@code null} when the header is missing
     */
    public Boolean getValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "ValidationReport{valid=" + valid + ", " + file.length + " bytes}";
    }
}
