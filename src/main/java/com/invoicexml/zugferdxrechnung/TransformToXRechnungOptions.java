package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for {@link ZugferdXRechnungClient#transformToXRechnung(FileUpload, String, TransformToXRechnungOptions)}.
 */
public final class TransformToXRechnungOptions {

    private PdfLanguage validationLanguage;

    /** Creates options with the defaults: findings in English. */
    public TransformToXRechnungOptions() {
    }

    /**
     * Returns the language of the findings of a rejected result.
     *
     * @return the language, or {@code null} for the default (English)
     */
    public PdfLanguage getValidationLanguage() {
        return validationLanguage;
    }

    /**
     * Sets the language of the findings of a rejected result. Rule ids and field
     * paths stay the same.
     *
     * @param validationLanguage the language, or {@code null} for English
     * @return this instance
     */
    public TransformToXRechnungOptions setValidationLanguage(PdfLanguage validationLanguage) {
        this.validationLanguage = validationLanguage;
        return this;
    }
}
