package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for {@link ZugferdXRechnungClient#validateZugferdReport(FileUpload, ReportOptions)}
 * and {@link ZugferdXRechnungClient#validateXRechnungReport(FileUpload, ReportOptions)}.
 */
public final class ReportOptions {

    private PdfLanguage validationLanguage;

    /** Creates options with the defaults: a report in English. */
    public ReportOptions() {
    }

    /**
     * Returns the language of the report.
     *
     * @return the language, or {@code null} for the default (English)
     */
    public PdfLanguage getValidationLanguage() {
        return validationLanguage;
    }

    /**
     * Sets the language of the whole report.
     *
     * @param validationLanguage the language, or {@code null} for English
     * @return this instance
     */
    public ReportOptions setValidationLanguage(PdfLanguage validationLanguage) {
        this.validationLanguage = validationLanguage;
        return this;
    }
}
