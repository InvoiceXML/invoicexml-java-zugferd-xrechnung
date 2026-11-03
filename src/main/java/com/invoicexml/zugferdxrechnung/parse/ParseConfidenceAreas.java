package com.invoicexml.zugferdxrechnung.parse;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Confidence scores per area of a PDF invoice reading, from 0.0 (had to guess)
 * to 1.0 (unambiguous). An area that was not scored is {@code null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ParseConfidenceAreas {

    @JsonProperty("sellerIdentification")
    private BigDecimal sellerIdentification;

    @JsonProperty("buyerIdentification")
    private BigDecimal buyerIdentification;

    @JsonProperty("taxCalculation")
    private BigDecimal taxCalculation;

    @JsonProperty("lineItems")
    private BigDecimal lineItems;

    /** Creates an empty instance. Used by Jackson. */
    public ParseConfidenceAreas() {
    }

    /**
     * Returns the score for identifying the seller.
     *
     * @return the score, or {@code null} when not scored
     */
    public BigDecimal getSellerIdentification() {
        return sellerIdentification;
    }

    /**
     * Returns the score for identifying the buyer.
     *
     * @return the score, or {@code null} when not scored
     */
    public BigDecimal getBuyerIdentification() {
        return buyerIdentification;
    }

    /**
     * Returns the score for the tax calculation.
     *
     * @return the score, or {@code null} when not scored
     */
    public BigDecimal getTaxCalculation() {
        return taxCalculation;
    }

    /**
     * Returns the score for the invoice lines. It is capped when the lines do not
     * add up to the printed totals.
     *
     * @return the score, or {@code null} when not scored
     */
    public BigDecimal getLineItems() {
        return lineItems;
    }

    @Override
    public String toString() {
        return "ParseConfidenceAreas{sellerIdentification=" + sellerIdentification
                + ", buyerIdentification=" + buyerIdentification
                + ", taxCalculation=" + taxCalculation
                + ", lineItems=" + lineItems + "}";
    }
}
