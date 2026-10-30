package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for {@link ZugferdXRechnungClient#transformToZugferd(FileUpload, TransformOptions)}.
 */
public final class TransformOptions {

    private PdfLanguage language;
    private Boolean preserveOriginalPdf;
    private Boolean includeAdvancedProperties;
    private PdfLanguage validationLanguage;

    /**
     * Creates options with the defaults: a PDF rendered fresh in the language the
     * invoice is written in, with the Electronic invoice details section.
     */
    public TransformOptions() {
    }

    /**
     * Returns the language of the rendered PDF.
     *
     * @return the language, or {@code null} for the language the invoice is written in
     */
    public PdfLanguage getLanguage() {
        return language;
    }

    /**
     * Sets the language of the rendered PDF and of the default texts in the embedded
     * XML. Left unset, the API uses the language the invoice is written in (its
     * {@code auto} value), and English when that is none of the three. With
     * {@link #setPreserveOriginalPdf(Boolean)} it words the XML texts only.
     *
     * @param language the language, or {@code null} for the language the invoice is written in
     * @return this instance
     */
    public TransformOptions setLanguage(PdfLanguage language) {
        this.language = language;
        return this;
    }

    /**
     * Returns whether the uploaded PDF stays the visual layer.
     *
     * @return the setting, or {@code null} for the default (off)
     */
    public Boolean getPreserveOriginalPdf() {
        return preserveOriginalPdf;
    }

    /**
     * Keeps the uploaded PDF as the visual layer and only embeds the XML built from
     * it. The pages stay exactly as they were. Earlier attachments and form fields
     * are not kept.
     *
     * @param preserveOriginalPdf {@code true} to keep the uploaded PDF, or {@code null} for the default (off)
     * @return this instance
     */
    public TransformOptions setPreserveOriginalPdf(Boolean preserveOriginalPdf) {
        this.preserveOriginalPdf = preserveOriginalPdf;
        return this;
    }

    /**
     * Returns whether the rendered PDF ends with the Electronic invoice details section.
     *
     * @return the setting, or {@code null} for the default (on)
     */
    public Boolean getIncludeAdvancedProperties() {
        return includeAdvancedProperties;
    }

    /**
     * Sets whether the rendered PDF ends with the Electronic invoice details section,
     * which lists the technical properties of the embedded XML so they can be checked
     * on the pages. On by default, set {@code false} for the classic layout alone.
     * Ignored with {@link #setPreserveOriginalPdf(Boolean)}.
     *
     * @param includeAdvancedProperties the setting, or {@code null} for the default (on)
     * @return this instance
     */
    public TransformOptions setIncludeAdvancedProperties(Boolean includeAdvancedProperties) {
        this.includeAdvancedProperties = includeAdvancedProperties;
        return this;
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
    public TransformOptions setValidationLanguage(PdfLanguage validationLanguage) {
        this.validationLanguage = validationLanguage;
        return this;
    }
}
