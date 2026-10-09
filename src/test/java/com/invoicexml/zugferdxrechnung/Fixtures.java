package com.invoicexml.zugferdxrechnung;

import com.invoicexml.zugferdxrechnung.model.BuyerParty;
import com.invoicexml.zugferdxrechnung.model.BuyerPostalAddress;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceLine;
import com.invoicexml.zugferdxrechnung.model.ItemInformation;
import com.invoicexml.zugferdxrechnung.model.LineVatInformation;
import com.invoicexml.zugferdxrechnung.model.PaymentDetails;
import com.invoicexml.zugferdxrechnung.model.PriceDetails;
import com.invoicexml.zugferdxrechnung.model.SellerParty;
import com.invoicexml.zugferdxrechnung.model.SellerPostalAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

final class Fixtures {

    private Fixtures() {
    }

    static InvoiceDocument invoice() {
        return new InvoiceDocument()
                .setInvoiceNumber("RE-2026-001")
                .setIssueDate(LocalDate.of(2026, 7, 25))
                .setCurrency("EUR")
                .setSeller(new SellerParty()
                        .setName("Mustermann Software GmbH")
                        .setVatIdentifier("DE123456789")
                        .setPostalAddress(new SellerPostalAddress()
                                .setLine1("Hauptstrasse 12").setCity("Berlin").setPostCode("10115").setCountry("DE")))
                .setBuyer(new BuyerParty()
                        .setName("Beispiel Handel AG")
                        .setPostalAddress(new BuyerPostalAddress()
                                .setLine1("Marienplatz 8").setCity("Muenchen").setPostCode("80331").setCountry("DE")))
                .setPaymentDetails(new PaymentDetails().setPaymentAccountIdentifier("DE89370400440532013000"))
                .addLine(new InvoiceLine()
                        .setQuantity(new BigDecimal("10"))
                        .setItem(new ItemInformation().setName("Softwareentwicklung"))
                        .setPriceDetails(new PriceDetails().setNetPrice(new BigDecimal("250.00")))
                        .setVatInformation(new LineVatInformation().setRate(new BigDecimal("19"))));
    }

    static String resource(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
