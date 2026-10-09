package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.PdfLanguage;

/**
 * Options for {@link ZugferdXRechnungClient#renderXRechnungToPdf(FileUpload, RenderOptions)}.
 */
public final class RenderOptions {

    private PdfLanguage language;
    private String logoUrl;

    /** Creates options with the defaults: English, no logo. */
    public RenderOptions() {
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
     * Sets the language of the rendered PDF.
     *
     * @param language the language, or {@code null} for English
     * @return this instance
     */
    public RenderOptions setLanguage(PdfLanguage language) {
        this.language = language;
        return this;
    }

    /**
     * Returns the logo URL.
     *
     * @return the URL, or {@code null}
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets a public URL of the seller's logo (PNG or JPEG, at most 2 MB), printed
     * top left on the rendered PDF.
     *
     * @param logoUrl the URL, or {@code null} for no logo
     * @return this instance
     */
    public RenderOptions setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }
}
