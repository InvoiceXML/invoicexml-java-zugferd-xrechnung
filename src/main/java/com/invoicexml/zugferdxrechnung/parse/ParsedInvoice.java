package com.invoicexml.zugferdxrechnung.parse;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;

/**
 * Result of reading a PDF invoice with AI: the invoice data, how sure the reading
 * is, and the language the invoice is written in. A person must check the data
 * before it is used.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class ParsedInvoice {

    @JsonProperty("invoice")
    private InvoiceDocument invoice;

    @JsonProperty("confidence")
    private ParseConfidence confidence;

    @JsonProperty("language")
    private String language;

    /** Creates an empty instance. Used by Jackson. */
    public ParsedInvoice() {
    }

    /**
     * Returns the invoice as read from the PDF.
     *
     * @return the invoice
     */
    public InvoiceDocument getInvoice() {
        return invoice;
    }

    /**
     * Returns how sure the reading is, overall and per area.
     *
     * @return the confidence, or {@code null} when the response carries none
     */
    public ParseConfidence getConfidence() {
        return confidence;
    }

    /**
     * Returns the language the invoice is written in, as a lowercase ISO 639-1
     * code such as {@code "de"}. Any language can come back, not only the ones
     * this library renders.
     *
     * @return the language code, or {@code null} when it is unknown
     */
    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "ParsedInvoice{invoiceNumber=" + (invoice == null ? null : invoice.getInvoiceNumber())
                + ", confidence=" + confidence + ", language=" + language + "}";
    }
}
