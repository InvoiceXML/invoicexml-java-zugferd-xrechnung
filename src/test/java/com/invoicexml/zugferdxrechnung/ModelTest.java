package com.invoicexml.zugferdxrechnung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoicexml.zugferdxrechnung.email.EmailMessage;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceNote;
import com.invoicexml.zugferdxrechnung.model.SellerIdentifier;
import com.invoicexml.zugferdxrechnung.model.XRechnungSyntax;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTest {

    private final ObjectMapper mapper = Json.newMapper();

    @Test
    void invoiceRoundTripsThroughJson() throws Exception {
        InvoiceDocument original = Fixtures.invoice()
                .addNote("Plain note")
                .addNote(InvoiceNote.of("Skonto", "AAB"));

        InvoiceDocument copy = mapper.readValue(mapper.writeValueAsBytes(original), InvoiceDocument.class);

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }

    @Test
    void notesAreSentAsObjects() throws Exception {
        String json = mapper.writeValueAsString(new InvoiceDocument().addNote("Hallo"));
        assertEquals("{\"notes\":[{\"note\":\"Hallo\"}]}", json);
    }

    @Test
    void identifierFactories() {
        assertEquals("0088", SellerIdentifier.of("4000001123452", "0088").getSchemeId());
        assertEquals(null, SellerIdentifier.of("HRB 12345").getSchemeId());
    }

    @Test
    void syntaxParsesCaseInsensitively() {
        assertEquals(XRechnungSyntax.CII, XRechnungSyntax.fromValue("CII"));
        assertThrows(IllegalArgumentException.class, () -> XRechnungSyntax.fromValue("pdf"));
    }

    @Test
    void emailMessageOmitsUnsetFieldsAndRoundTrips() throws Exception {
        EmailMessage required = EmailMessage.of("invoices@example.com", "ap@example.org");
        assertEquals("{\"from\":\"invoices@example.com\",\"to\":\"ap@example.org\"}", mapper.writeValueAsString(required));

        EmailMessage original = EmailMessage.of("invoices@example.com", "ap@example.org")
                .setFromName("Billing")
                .addCc("cc@example.org")
                .addBcc("bcc@example.org")
                .setReplyTo("help@example.com")
                .setSubject("Rechnung RE-2026-001")
                .setMessage("Guten Tag");

        EmailMessage copy = mapper.readValue(mapper.writeValueAsBytes(original), EmailMessage.class);

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }

    @Test
    void emailMessageToStringRevealsNoAddressOrText() {
        String printed = EmailMessage.of("invoices@example.com", "ap@example.org")
                .setFromName("Billing")
                .addCc("cc@example.org")
                .addBcc("bcc@example.org")
                .setReplyTo("help@example.com")
                .setSubject("Rechnung RE-2026-001")
                .setMessage("Guten Tag")
                .toString();

        for (String secret : List.of("invoices@example.com", "ap@example.org", "Billing", "cc@example.org",
                "bcc@example.org", "help@example.com", "Rechnung RE-2026-001", "Guten Tag")) {
            assertFalse(printed.contains(secret), secret);
        }
    }

    @Test
    void retryAfterParsing() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
        assertEquals(Duration.ofSeconds(7), RetryAfter.parse(" 7 ", clock));
        assertEquals(Duration.ZERO, RetryAfter.parse("Fri, 09 Oct 2026 11:00:00 GMT", clock));
        assertEquals(null, RetryAfter.parse("soon", clock));
        assertEquals(null, RetryAfter.parse("-3", clock));
        assertEquals(null, RetryAfter.parse(null, clock));
    }
}
