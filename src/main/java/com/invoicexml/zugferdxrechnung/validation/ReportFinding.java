package com.invoicexml.zugferdxrechnung.validation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A finding attached to one row of the field report: the rule that fired and its message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ReportFinding {

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("message")
    private String message;

    /** Creates an empty instance. Used by Jackson. */
    public ReportFinding() {
    }

    /**
     * Returns the rule identifier, for example {@code "BR-CO-15"}.
     *
     * @return the rule identifier
     */
    public String getRule() {
        return rule;
    }

    /**
     * Returns the plain-language message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + rule + "] " + message;
    }
}
