// Creates a ZUGFeRD invoice and emails it to your customer in one call, sent from your own address.
//
// Before running it:
// - Add and verify your sending domain on the Verified email domains page of your dashboard.
//   Until it is verified, the API refuses the call and nothing is sent.
// - The client already retries when the API answers 429, because nothing was sent then.
//   A timeout or a lost connection may still mean the email went out, so check before
//   sending the same invoice again.

import com.invoicexml.zugferdxrechnung.ErrorCodes;
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
import java.math.BigDecimal;
import java.util.Objects;

public class EmailZugferd {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String FROM = "invoices@yourcompany.example"; // must be on your verified domain
    private static final String TO = "accounting@customer.example";

    public static void main(String[] args) {
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
            // The invoice is validated exactly like on createZugferd before anything is sent.
            EmailSendResult sent = client.emailZugferd(invoice, email, new InvoiceOptions().setLanguage(PdfLanguage.DE));

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
