// Creates a ZUGFeRD invoice that keeps your own PDF layout.
// The API downloads your PDF from a public URL, embeds the XML it generates from the invoice
// data into it, and returns a ZUGFeRD PDF/A-3. Nothing about your PDF's look changes.

import com.invoicexml.zugferdxrechnung.InvoiceValidationException;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.BuyerParty;
import com.invoicexml.zugferdxrechnung.model.BuyerPostalAddress;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceLine;
import com.invoicexml.zugferdxrechnung.model.InvoiceOptions;
import com.invoicexml.zugferdxrechnung.model.ItemInformation;
import com.invoicexml.zugferdxrechnung.model.LineVatInformation;
import com.invoicexml.zugferdxrechnung.model.PriceDetails;
import com.invoicexml.zugferdxrechnung.model.SellerParty;
import com.invoicexml.zugferdxrechnung.model.SellerPostalAddress;
import com.invoicexml.zugferdxrechnung.model.VatCategoryCodes;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateZugferdWithOwnPdf {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String PDF_URL = "https://example.com/invoices/RE-2026-001.pdf"; // public http(s) URL of your PDF
    private static final String OUTPUT_PATH = "RE-2026-001-zugferd.pdf";

    public static void main(String[] args) throws IOException {
        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        InvoiceDocument invoice = new InvoiceDocument()
                .setInvoiceNumber("RE-2026-001")
                .setCurrency("EUR")
                .setSeller(new SellerParty()
                        .setName("Muster GmbH")
                        .setVatIdentifier("DE123456789")
                        .setPostalAddress(new SellerPostalAddress().setCity("Berlin").setPostCode("10115").setCountry("DE")))
                .setBuyer(new BuyerParty()
                        .setName("Kunde AG")
                        .setPostalAddress(new BuyerPostalAddress().setCity("Hamburg").setPostCode("20095").setCountry("DE")))
                .addLine(new InvoiceLine()
                        .setQuantity(new BigDecimal("10"))
                        .setUnitCode("HUR") // hour
                        .setItem(new ItemInformation().setName("Consulting"))
                        .setPriceDetails(new PriceDetails().setNetPrice(new BigDecimal("95.50")))
                        .setVatInformation(new LineVatInformation()
                                .setCategoryCode(VatCategoryCodes.STANDARD_RATE)
                                .setRate(new BigDecimal("19"))));

        try {
            // The PDF URL makes your PDF the visual layer. Without it, the API renders its own invoice layout.
            byte[] zugferd = client.createZugferd(invoice, new InvoiceOptions().setPdfUrl(PDF_URL));

            Files.write(Path.of(OUTPUT_PATH), zugferd);
            System.out.println("Saved the ZUGFeRD invoice to " + OUTPUT_PATH + ".");
        } catch (InvoiceValidationException e) {
            // The invoice data would not produce a compliant ZUGFeRD invoice. Each finding names the field to fix.
            System.out.println("The invoice is not compliant:");
            for (ValidationFinding finding : e.getErrors()) {
                System.out.println("  [" + finding.getRule() + "] " + finding.getMessage()
                        + " (" + String.join(", ", finding.getFields()) + ")");
            }
        } catch (InvoiceXmlApiException e) {
            // For example an invalid API key, or a PDF URL the API could not download.
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }
}
