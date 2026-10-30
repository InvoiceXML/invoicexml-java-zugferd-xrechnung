package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for the convert calls that return a ZUGFeRD PDF, such as
 * {@link ZugferdXRechnungClient#convertCiiToZugferd(FileUpload, ConvertToPdfOptions)}.
 */
public final class ConvertToPdfOptions {

    private PdfLanguage language;
    private PdfLanguage validationLanguage;

    /** Creates options with the defaults: PDF and findings in English. */
    public ConvertToPdfOptions() {
    }

    /**
     * Returns the language of the rendered PDF.
     *
     * @return the language, or {@code null} for the default (English)
     */
    public PdfLanguage getLanguage() {
        return language;
    }

    /**
     * Sets the language of the rendered PDF. It also words the default texts the
     * XML gains when the source has none, such as the payment terms.
     *
     * @param language the language, or {@code null} for English
     * @return this instance
     */
    public ConvertToPdfOptions setLanguage(PdfLanguage language) {
        this.language = language;
        return this;
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
    public ConvertToPdfOptions setValidationLanguage(PdfLanguage validationLanguage) {
        this.validationLanguage = validationLanguage;
        return this;
    }
}
