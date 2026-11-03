// Emails a PDF you already have as a ZUGFeRD invoice, sent from your own address.
// The CII XML is built from your invoice data, then embedded into your PDF and emailed in
// one call. Use this when your own system prints the invoice PDF.
//
// Before running it:
// - Add and verify your sending domain on the Verified email domains page of your dashboard.
//   Until it is verified, the API refuses the call and nothing is sent.
// - The client already retries when the API answers 429, because nothing was sent then.
//   A timeout or a lost connection may still mean the email went out, so check before
//   sending the same invoice again.

import com.invoicexml.zugferdxrechnung.ErrorCodes;
import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceValidationException;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.email.EmailMessage;
import com.invoicexml.zugferdxrechnung.email.EmailSendResult;
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
import java.util.Objects;

public class EmailEmbeddedZugferd {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String PDF_PATH = "invoices/RE-2026-001.pdf";     // the PDF your system printed
    private static final String FROM = "invoices@yourcompany.example";    // must be on your verified domain
    private static final String TO = "accounting@customer.example";

    public static void main(String[] args) throws IOException {
        // Read the PDF first, so a wrong path stops the example before any API call.
        FileUpload pdf;
        try {
            Path path = Path.of(PDF_PATH);
            pdf = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + PDF_PATH + ".");
            System.out.println("Set PDF_PATH at the top of EmailEmbeddedZugferd.java to a PDF on your machine.");
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

        EmailMessage email = EmailMessage.of(FROM, TO)
                .setFromName("Muster GmbH Billing")
                .setSubject("Rechnung RE-2026-001")
                .setMessage("Anbei erhalten Sie Ihre Rechnung.");

        try {
            // 1. Build the CII XML from the invoice data. The language words the default texts, such as the payment terms.
            String cii = client.createCii(invoice, new InvoiceOptions().setLanguage(PdfLanguage.DE));

            // 2. Embed the XML into your PDF and email the ZUGFeRD invoice. Both are validated before anything is sent.
            EmailSendResult sent = client.emailEmbeddedZugferd(pdf, FileUpload.ofXml(cii), email);

            // "accepted" means the email was handed over for delivery.
            System.out.println(sent.getStatus() + ": message id " + sent.getMessageId());
        } catch (InvoiceValidationException e) {
            // The invoice or an email field is invalid. Nothing was sent.
            System.out.println("Nothing was sent. Fix these fields first:");
            for (ValidationFinding finding : e.getErrors()) {
                System.out.println("  [" + finding.getRule() + "] " + finding.getMessage()
                        + " (" + String.join(", ", finding.getFields()) + ")");
            }
        } catch (InvoiceXmlApiException e) {
            if (Objects.equals(e.getErrorCode(), ErrorCodes.EMAIL_DOMAIN_NOT_VERIFIED)) {
                System.out.println("The domain of " + FROM + " is not verified yet. Verify it in your dashboard, then run this again.");
            } else {
                System.out.println("The API refused the request: " + e.getMessage());
            }
        }
    }
}
