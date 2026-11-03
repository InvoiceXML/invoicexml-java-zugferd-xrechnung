package com.invoicexml.zugferdxrechnung.parse;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * How sure the reading of a PDF invoice is. Scores run from 0.0 (had to guess)
 * to 1.0 (unambiguous).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ParseConfidence {

    @JsonProperty("overall")
    private BigDecimal overall;

    @JsonProperty("areas")
    private ParseConfidenceAreas areas;

    /** Creates an empty instance. Used by Jackson. */
    public ParseConfidence() {
    }

    /**
     * Returns the overall score, the mean of the reported areas.
     *
     * @return the score, or {@code null} when no area was scored
     */
    public BigDecimal getOverall() {
        return overall;
    }

    /**
     * Returns the scores per area.
     *
     * @return the areas, or {@code null} when the response carries none
     */
    public ParseConfidenceAreas getAreas() {
        return areas;
    }

    @Override
    public String toString() {
        return "ParseConfidence{overall=" + overall + ", areas=" + areas + "}";
    }
}
