// Builds a CII XML from your invoice data, then embeds it into a PDF you already have.
// Use this when your own system prints the invoice PDF and you want it to become a
// ZUGFeRD invoice. The CII XML declares the EN 16931 profile, so it embeds as it is.

import com.invoicexml.zugferdxrechnung.FileUpload;
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
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.model.PriceDetails;
import com.invoicexml.zugferdxrechnung.model.SellerParty;
import com.invoicexml.zugferdxrechnung.model.SellerPostalAddress;
import com.invoicexml.zugferdxrechnung.model.VatCategoryCodes;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class CreateCiiAndEmbed {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String PDF_PATH = "invoices/RE-2026-001.pdf"; // the PDF your system printed
    private static final String OUTPUT_PATH = "RE-2026-001-zugferd.pdf";

    public static void main(String[] args) throws IOException {
        // Read the PDF first, so a wrong path stops the example before any API call.
        FileUpload pdf;
        try {
            Path path = Path.of(PDF_PATH);
            pdf = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + PDF_PATH + ".");
            System.out.println("Set PDF_PATH at the top of CreateCiiAndEmbed.java to a PDF on your machine.");
            return;
        }

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
            // 1. Build the CII XML from the invoice data. The language words the default texts, such as the payment terms.
            String cii = client.createCii(invoice, new InvoiceOptions().setLanguage(PdfLanguage.DE));

            // 2. Embed the XML into your PDF. The result is a ZUGFeRD PDF/A-3.
            byte[] zugferd = client.embedZugferd(pdf, FileUpload.ofXml(cii));

            Files.write(Path.of(OUTPUT_PATH), zugferd);
            System.out.println("Saved the ZUGFeRD invoice to " + OUTPUT_PATH + ".");
        } catch (InvoiceValidationException e) {
            // The invoice data would not produce a compliant invoice. Each finding names the field to fix.
            System.out.println("The invoice is not compliant:");
            for (ValidationFinding finding : e.getErrors()) {
                System.out.println("  [" + finding.getRule() + "] " + finding.getMessage()
                        + " (" + String.join(", ", finding.getFields()) + ")");
            }
        } catch (InvoiceXmlApiException e) {
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }
}
