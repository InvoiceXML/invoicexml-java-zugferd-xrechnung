package com.invoicexml.zugferdxrechnung.email;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of an email call. The invoice went to the recipient, so no file comes back.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class EmailSendResult {

    @JsonProperty("status")
    private String status;

    @JsonProperty("messageId")
    private String messageId;

    /** Creates an empty instance. Used by Jackson. */
    public EmailSendResult() {
    }

    /**
     * Returns the delivery status, always {@code "accepted"}: the email was handed
     * over for delivery. It does not mean the recipient's server has received it yet.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the identifier of the sent email. Keep it to ask about a delivery later.
     *
     * @return the message id
     */
    public String getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "EmailSendResult{status=" + status + ", messageId=" + messageId + "}";
    }
}
