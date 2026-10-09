package com.invoicexml.zugferdxrechnung.validation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation call.
 *
 * <p>Valid and invalid invoices both come back as a result, so branch on
 * {@link #isValid()}. Only transport problems (missing file, unreadable XML,
 * authentication) raise an exception.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ValidationResult {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("detail")
    private String detail;

    @JsonProperty("data")
    private ValidationData data;

    @JsonProperty("errors")
    private List<ValidationFinding> errors;

    @JsonProperty("warnings")
    private List<ValidationFinding> warnings;

    @JsonProperty("report")
    private List<ReportRow> report;

    /** Creates an empty instance. Used by Jackson. */
    public ValidationResult() {
    }

    /**
     * Returns whether the invoice is compliant. Warnings never make an invoice invalid.
     *
     * @return {@code true} when there are no errors
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns a one-sentence summary of the verdict.
     *
     * @return the summary
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns the structured verdict details: schema and Schematron flags,
     * conformance level, detected profile and declared specification identifier.
     *
     * @return the details, or {@code null} when not provided
     */
    public ValidationData getData() {
        return data;
    }

    /**
     * Returns the blocking findings.
     *
     * @return the errors, empty when the invoice is valid
     */
    public List<ValidationFinding> getErrors() {
        return errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
    }

    /**
     * Returns the advisory findings. They never block.
     *
     * @return the warnings, never {@code null}
     */
    public List<ValidationFinding> getWarnings() {
        return warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
    }

    /**
     * Returns the field report: one row per invoice field with its value and
     * the findings that reference it.
     *
     * @return the rows, empty when the invoice could not be parsed into fields
     */
    public List<ReportRow> getReport() {
        return report == null ? Collections.emptyList() : Collections.unmodifiableList(report);
    }

    @Override
    public String toString() {
        return "ValidationResult{valid=" + valid + ", errors=" + getErrors().size()
                + ", warnings=" + getWarnings().size() + ", detail=" + detail + "}";
    }
}
