package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for the convert calls that return XML, such as
 * {@link ZugferdXRechnungClient#convertZugferdToUbl(FileUpload, ConvertOptions)}.
 */
public final class ConvertOptions {

    private PdfLanguage validationLanguage;

    /** Creates options with the defaults: findings in English. */
    public ConvertOptions() {
    }

    /**
     * Returns the language of the findings of a failed conversion.
     *
     * @return the language, or {@code null} for the default (English)
     */
    public PdfLanguage getValidationLanguage() {
        return validationLanguage;
    }

    /**
     * Sets the language of the findings of a failed conversion. Rule ids and
     * field paths stay the same.
     *
     * @param validationLanguage the language, or {@code null} for English
     * @return this instance
     */
    public ConvertOptions setValidationLanguage(PdfLanguage validationLanguage) {
        this.validationLanguage = validationLanguage;
        return this;
    }
}
