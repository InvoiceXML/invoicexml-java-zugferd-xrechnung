package com.invoicexml.zugferdxrechnung;

/**
 * Thrown when the API could not be reached or the response could not be read:
 * DNS or connect failures, timeouts, or an interrupted calling thread.
 */
public class InvoiceXmlConnectionException extends InvoiceXmlException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message the message
     * @param cause the underlying I/O or interruption cause
     */
    public InvoiceXmlConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
