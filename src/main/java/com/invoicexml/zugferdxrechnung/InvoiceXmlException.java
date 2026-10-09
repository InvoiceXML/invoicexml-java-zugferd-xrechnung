package com.invoicexml.zugferdxrechnung;

/**
 * Base class of every exception thrown by this library. Unchecked.
 *
 * <p>Subclasses: {@link InvoiceXmlApiException} for error responses from the
 * API, and {@link InvoiceXmlConnectionException} when no response was received.
 */
public class InvoiceXmlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message the message
     * @param cause the cause, or {@code null}
     */
    public InvoiceXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
