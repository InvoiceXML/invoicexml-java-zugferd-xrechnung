package com.invoicexml.zugferdxrechnung.email;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The email that carries an invoice: sender, recipient and optional texts.
 *
 * <p>Required by the API: {@code from} and {@code to}. The sender must be on a
 * domain added and verified on the Verified email domains page of the dashboard.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class EmailMessage {

    @JsonProperty("from")
    private String from;

    @JsonProperty("fromName")
    private String fromName;

    @JsonProperty("to")
    private String to;

    @JsonProperty("cc")
    private List<String> cc;

    @JsonProperty("bcc")
    private List<String> bcc;

    @JsonProperty("replyTo")
    private String replyTo;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("message")
    private String message;

    /** Creates an empty instance. */
    public EmailMessage() {
    }

    /**
     * Creates an email with its required fields.
     *
     * @param from the sender address
     * @param to the recipient address
     * @return the new instance
     */
    public static EmailMessage of(String from, String to) {
        return new EmailMessage().setFrom(from).setTo(to);
    }

    /**
     * Returns the value of {@code from}. The sender address, on one of your verified domains. Required.
     *
     * @return the value, or {@code null} when not set
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets {@code from}. The sender address, on one of your verified domains. Required.
     *
     * @param from the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * Returns the value of {@code fromName}. Display name of the sender.
     *
     * @return the value, or {@code null} when not set
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * Sets {@code fromName}. Display name of the sender.
     *
     * @param fromName the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setFromName(String fromName) {
        this.fromName = fromName;
        return this;
    }

    /**
     * Returns the value of {@code to}. The recipient's address. One address. Required.
     *
     * @return the value, or {@code null} when not set
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets {@code to}. The recipient's address. One address. Required.
     *
     * @param to the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setTo(String to) {
        this.to = to;
        return this;
    }

    /**
     * Returns the value of {@code cc}. Addresses that receive a visible copy.
     *
     * @return the value, or {@code null} when not set
     */
    public List<String> getCc() {
        return cc;
    }

    /**
     * Sets {@code cc}. Addresses that receive a visible copy. {@code cc} and {@code bcc} hold at most 2 addresses together.
     *
     * @param cc the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setCc(List<String> cc) {
        this.cc = cc;
        return this;
    }

    /**
     * Appends one entry to {@code cc}.
     *
     * @param value the entry to append
     * @return this instance
     */
    public EmailMessage addCc(String value) {
        if (cc == null) {
            cc = new ArrayList<>();
        }
        cc.add(value);
        return this;
    }

    /**
     * Returns the value of {@code bcc}. Addresses that receive a hidden copy.
     *
     * @return the value, or {@code null} when not set
     */
    public List<String> getBcc() {
        return bcc;
    }

    /**
     * Sets {@code bcc}. Addresses that receive a hidden copy. {@code cc} and {@code bcc} hold at most 2 addresses together.
     *
     * @param bcc the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setBcc(List<String> bcc) {
        this.bcc = bcc;
        return this;
    }

    /**
     * Appends one entry to {@code bcc}.
     *
     * @param value the entry to append
     * @return this instance
     */
    public EmailMessage addBcc(String value) {
        if (bcc == null) {
            bcc = new ArrayList<>();
        }
        bcc.add(value);
        return this;
    }

    /**
     * Returns the value of {@code replyTo}. The address the recipient's replies go to, when it differs from the sender. Any domain.
     *
     * @return the value, or {@code null} when not set
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets {@code replyTo}. The address the recipient's replies go to, when it differs from the sender. Any domain.
     *
     * @param replyTo the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setReplyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    /**
     * Returns the value of {@code subject}. The subject line. When omitted it is the document type and its number.
     *
     * @return the value, or {@code null} when not set
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets {@code subject}. The subject line. When omitted it is the document type and its number.
     *
     * @param subject the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Returns the value of {@code message}. Plain-text message, sent exactly as given. When omitted a short English text names the attached document and its number.
     *
     * @return the value, or {@code null} when not set
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets {@code message}. Plain-text message, sent exactly as given. When omitted a short English text names the attached document and its number.
     *
     * @param message the new value, or {@code null} to omit it
     * @return this instance
     */
    public EmailMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmailMessage)) {
            return false;
        }
        EmailMessage that = (EmailMessage) o;
        return Objects.equals(from, that.from)
                && Objects.equals(fromName, that.fromName)
                && Objects.equals(to, that.to)
                && Objects.equals(cc, that.cc)
                && Objects.equals(bcc, that.bcc)
                && Objects.equals(replyTo, that.replyTo)
                && Objects.equals(subject, that.subject)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, fromName, to, cc, bcc, replyTo, subject, message);
    }

    /** Hides every value, so printing or logging this object reveals no address or text. */
    @Override
    public String toString() {
        return "EmailMessage{hidden}";
    }
}
