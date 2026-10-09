package com.invoicexml.zugferdxrechnung.validation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row of the field report: an invoice field, its value on the parsed
 * invoice, and the findings that reference it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ReportRow {

    @JsonProperty("name")
    private String name;

    @JsonProperty("section")
    private String section;

    @JsonProperty("path")
    private String path;

    @JsonProperty("value")
    private String value;

    @JsonProperty("isValid")
    private boolean valid;

    @JsonProperty("errors")
    private List<ReportFinding> errors;

    @JsonProperty("warnings")
    private List<ReportFinding> warnings;

    @JsonAnySetter
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    /** Creates an empty instance. Used by Jackson. */
    public ReportRow() {
    }

    /**
     * Returns the field name, for example {@code "Invoice number"}.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the logical section: {@code header}, {@code seller}, {@code buyer},
     * {@code lines}, {@code payment}, {@code totals} or {@code tax}.
     *
     * @return the section
     */
    public String getSection() {
        return section;
    }

    /**
     * Returns the field path into the invoice document, for example
     * {@code lines[0].priceDetails.netPrice}.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the field value for display. Long values are truncated and binary
     * attachments summarised.
     *
     * @return the value, or {@code null} when the field is absent
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns whether no error references this field.
     *
     * @return {@code true} when the row has no errors
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the errors that reference this field.
     *
     * @return the errors, never {@code null}
     */
    public List<ReportFinding> getErrors() {
        return errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
    }

    /**
     * Returns the warnings that reference this field.
     *
     * @return the warnings, never {@code null}
     */
    public List<ReportFinding> getWarnings() {
        return warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
    }

    /**
     * Returns response fields this version of the library does not model.
     *
     * @return the extra fields by name, never {@code null}
     */
    public Map<String, Object> getAdditionalProperties() {
        return Collections.unmodifiableMap(additionalProperties);
    }
}
