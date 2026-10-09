package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when the API rejects a request with HTTP 400 and a list of findings.
 *
 * <p>On the create endpoints this means the invoice failed the rule set before
 * any document was produced, so a non-compliant invoice never leaves your
 * system. {@link #getErrors()} lists the violated rules with the affected
 * fields. {@link ErrorCodes#XML_VALIDATION} marks business rule violations of
 * the generated XML, {@link ErrorCodes#MODEL_VALIDATION} marks missing or
 * malformed request fields.
 */
public class InvoiceValidationException extends InvoiceXmlApiException {

    private static final long serialVersionUID = 1L;

    private final transient List<ValidationFinding> errors;
    private final transient List<ValidationFinding> warnings;
    private final transient InvoiceDocument invoiceData;

    /**
     * Creates an exception.
     *
     * @param statusCode the HTTP status code
     * @param errorCode the API error code, or {@code null}
     * @param title the problem title, or {@code null}
     * @param detail the problem detail, or {@code null}
     * @param traceId the trace identifier, or {@code null}
     * @param responseBody the raw response body
     * @param errors the blocking findings
     * @param warnings the advisory findings
     * @param invoiceData the invoice as the API parsed it, or {@code null}
     */
    public InvoiceValidationException(int statusCode, Integer errorCode, String title, String detail,
            String traceId, String responseBody, List<ValidationFinding> errors,
            List<ValidationFinding> warnings, InvoiceDocument invoiceData) {
        super(statusCode, errorCode, title, detail, traceId, responseBody);
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
        this.invoiceData = invoiceData;
    }

    /**
     * Returns the blocking findings.
     *
     * @return the findings, never {@code null}
     */
    public List<ValidationFinding> getErrors() {
        return errors;
    }

    /**
     * Returns the advisory findings.
     *
     * @return the findings, never {@code null}
     */
    public List<ValidationFinding> getWarnings() {
        return warnings;
    }

    /**
     * Returns the invoice as the API parsed it, including calculated defaults,
     * so a caller correcting field by field can patch the latest state.
     *
     * @return the parsed invoice, or {@code null} when the request body could not be read
     */
    public InvoiceDocument getInvoiceData() {
        return invoiceData;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        for (ValidationFinding f : errors) {
            sb.append(System.lineSeparator()).append("  ").append(f);
        }
        return sb.toString();
    }
}
