package com.invoicexml.zugferdxrechnung;

/**
 * Options for {@link ZugferdXRechnungClient#embedZugferd(FileUpload, FileUpload, EmbedOptions)}.
 */
public final class EmbedOptions {

    private boolean skipValidation;

    /** Creates options with the defaults: full validation before embedding. */
    public EmbedOptions() {
    }

    /**
     * Returns whether business rule validation is skipped.
     *
     * @return {@code true} for packaging-only mode
     */
    public boolean isSkipValidation() {
        return skipValidation;
    }

    /**
     * Selects packaging-only mode. By default the XML runs through the full rule
     * set of {@code /v1/validate/zugferd} before it is embedded, and an invoice
     * with errors is rejected. With {@code true} only the structural profile
     * schema check runs, for callers who knowingly package transitional XML.
     *
     * @param skipValidation {@code true} to skip the business rules
     * @return this instance
     */
    public EmbedOptions setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
        return this;
    }
}
