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
 * One self-contained validation finding: the rule that fired, the layer that
 * produced it, a plain-language message, the affected field paths, and the
 * verbatim validator output.
 *
 * <p>The same shape is used for validation responses and for the findings of a
 * rejected create request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ValidationFinding {

    /** Layer slug for XML Schema (XSD) structural findings. */
    public static final String LAYER_XSD = "xsd";
    /** Layer slug for EN 16931 business rule findings. */
    public static final String LAYER_EN16931 = "en16931";
    /** Layer slug for national CIUS findings, such as the XRechnung BR-DE rules. */
    public static final String LAYER_CIUS = "cius";

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("layer")
    private String layer;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("message")
    private String message;

    @JsonProperty("fields")
    private List<String> fields;

    @JsonProperty("raw")
    private String raw;

    @JsonAnySetter
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    /** Creates an empty finding. Used by Jackson. */
    public ValidationFinding() {
    }

    /**
     * Returns the rule identifier, for example {@code "BR-CO-15"} or {@code "BR-DE-15"}.
     * Stable across releases, so it is safe to key translations or documentation on it.
     *
     * @return the rule identifier, or {@code null} for request model errors
     */
    public String getRule() {
        return rule;
    }

    /**
     * Returns the layer that produced the finding: {@value #LAYER_XSD},
     * {@value #LAYER_EN16931} or {@value #LAYER_CIUS}.
     *
     * @return the layer slug, or {@code null} for request model errors
     */
    public String getLayer() {
        return layer;
    }

    /**
     * Returns the 1-based invoice line number the finding refers to.
     *
     * @return the line number, or {@code null} for document level findings
     */
    public Integer getLine() {
        return line;
    }

    /**
     * Returns the plain-language message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the field paths into the invoice document, for example
     * {@code lines[0].priceDetails.netPrice}.
     *
     * @return the paths, never {@code null}
     */
    public List<String> getFields() {
        return fields == null ? Collections.emptyList() : Collections.unmodifiableList(fields);
    }

    /**
     * Returns the verbatim validator output.
     *
     * @return the raw text, or {@code null} when not available
     */
    public String getRaw() {
        return raw;
    }

    /**
     * Returns response fields this version of the library does not model.
     *
     * @return the extra fields by name, never {@code null}
     */
    public Map<String, Object> getAdditionalProperties() {
        return Collections.unmodifiableMap(additionalProperties);
    }

    @Override
    public String toString() {
        return (rule == null ? "" : "[" + rule + "] ") + message;
    }
}
