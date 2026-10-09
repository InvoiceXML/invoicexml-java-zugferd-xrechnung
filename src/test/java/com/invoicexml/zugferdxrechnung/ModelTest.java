package com.invoicexml.zugferdxrechnung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceNote;
import com.invoicexml.zugferdxrechnung.model.SellerIdentifier;
import com.invoicexml.zugferdxrechnung.model.XRechnungSyntax;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
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
    void retryAfterParsing() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
        assertEquals(Duration.ofSeconds(7), RetryAfter.parse(" 7 ", clock));
        assertEquals(Duration.ZERO, RetryAfter.parse("Fri, 09 Oct 2026 11:00:00 GMT", clock));
        assertEquals(null, RetryAfter.parse("soon", clock));
        assertEquals(null, RetryAfter.parse("-3", clock));
        assertEquals(null, RetryAfter.parse(null, clock));
    }
}
