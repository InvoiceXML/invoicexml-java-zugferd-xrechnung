package com.invoicexml.zugferdxrechnung.validation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured verdict details of a validation run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ValidationData {

    @JsonProperty("hasEmbeddedXml")
    private Boolean hasEmbeddedXml;

    @JsonProperty("schemaValid")
    private Boolean schemaValid;

    @JsonProperty("schematronValid")
    private Boolean schematronValid;

    @JsonProperty("conformanceLevel")
    private String conformanceLevel;

    @JsonProperty("profile")
    private String profile;

    @JsonProperty("customizationId")
    private String customizationId;

    /** Creates an empty instance. Used by Jackson. */
    public ValidationData() {
    }

    /**
     * Returns whether the uploaded PDF carries an embedded invoice XML. Only set
     * when validating a hybrid PDF (ZUGFeRD).
     *
     * @return the flag, or {@code null} for XML uploads
     */
    public Boolean getHasEmbeddedXml() {
        return hasEmbeddedXml;
    }

    /**
     * Returns whether the XML passed the XML Schema (XSD) structural checks.
     *
     * @return the flag, or {@code null} when that step did not run
     */
    public Boolean getSchemaValid() {
        return schemaValid;
    }

    /**
     * Returns whether the XML passed the Schematron business rules (EN 16931 and,
     * where applicable, the national CIUS rules).
     *
     * @return the flag, or {@code null} when that step did not run
     */
    public Boolean getSchematronValid() {
        return schematronValid;
    }

    /**
     * Returns the conformance level validated against, for example {@code "EN16931"}
     * or {@code "XRechnung"}.
     *
     * @return the conformance level, or {@code null}
     */
    public String getConformanceLevel() {
        return conformanceLevel;
    }

    /**
     * Returns the slug of the profile whose rule set was applied, for example
     * {@code "en16931"}, {@code "basic"} or {@code "xrechnung"}.
     *
     * @return the profile slug, or {@code null} when the profile was not recognised
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Returns the specification identifier the document declares.
     *
     * @return the declared identifier, or {@code null}
     */
    public String getCustomizationId() {
        return customizationId;
    }
}
