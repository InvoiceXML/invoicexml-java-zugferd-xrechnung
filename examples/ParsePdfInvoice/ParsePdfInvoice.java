// Reads an ordinary PDF invoice, typed, scanned or photographed, into the InvoiceDocument model.
// AI reads the PDF, so a person must check the data before it is used. The result says how sure
// the read was per area, which tells the person where to look first.
// To read a ZUGFeRD PDF or an XRechnung XML, use the ExtractJsonAndAttachments example instead:
// it reads the embedded XML exactly, without AI.

import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.RateLimitException;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceLine;
import com.invoicexml.zugferdxrechnung.parse.ParseConfidence;
import com.invoicexml.zugferdxrechnung.parse.ParsedInvoice;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class ParsePdfInvoice {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String PDF_PATH = "invoices/supplier-invoice.pdf"; // an ordinary PDF invoice
    private static final BigDecimal CHECK_BELOW = new BigDecimal("0.8"); // areas scored below this are flagged for a closer check

    public static void main(String[] args) throws IOException {
        // Read the PDF first, so a wrong path stops the example before any API call.
        FileUpload pdf;
        try {
            Path path = Path.of(PDF_PATH);
            pdf = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + PDF_PATH + ".");
            System.out.println("Set PDF_PATH at the top of ParsePdfInvoice.java to a PDF invoice on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            ParsedInvoice parsed = client.parseJson(pdf);
            InvoiceDocument invoice = parsed.getInvoice();

            System.out.println("Invoice " + invoice.getInvoiceNumber()
                    + " from " + (invoice.getSeller() == null ? null : invoice.getSeller().getName())
                    + " to " + (invoice.getBuyer() == null ? null : invoice.getBuyer().getName()));
            if (invoice.getTotals() != null) {
                System.out.println("Total " + invoice.getTotals().getGrandTotalAmount() + " " + invoice.getCurrency()
                        + ", due " + invoice.getTotals().getDuePayableAmount());
            }
            if (invoice.getLines() != null) {
                for (InvoiceLine line : invoice.getLines()) {
                    System.out.println("  " + line.getQuantity() + " x "
                            + (line.getItem() == null ? null : line.getItem().getName())
                            + " at " + (line.getPriceDetails() == null ? null : line.getPriceDetails().getNetPrice()));
                }
            }

            // The language the invoice is written in, as an ISO 639-1 code such as "de". Null when unknown.
            System.out.println("Language: " + (parsed.getLanguage() == null ? "unknown" : parsed.getLanguage()));

            // Scores run from 0.0 (had to guess) to 1.0 (unambiguous). They can be missing.
            ParseConfidence confidence = parsed.getConfidence();
            if (confidence != null) {
                System.out.println("Confidence overall: " + confidence.getOverall());
                if (confidence.getAreas() != null) {
                    flag("seller", confidence.getAreas().getSellerIdentification());
                    flag("buyer", confidence.getAreas().getBuyerIdentification());
                    flag("VAT", confidence.getAreas().getTaxCalculation());
                    flag("lines", confidence.getAreas().getLineItems());
                }
            }

            // The checked invoice can be passed straight to a create method, for example
            // client.createZugferd(invoice) to turn it into a ZUGFeRD invoice.
        } catch (RateLimitException e) {
            // Parse calls are not retried automatically, because a retry repeats the AI read.
            System.out.println(e.getRetryAfter() != null
                    ? "Too many requests. Wait " + e.getRetryAfter().getSeconds() + " seconds before trying again."
                    : "Too many requests. Wait a moment before trying again.");
        } catch (InvoiceXmlApiException e) {
            // For example an invalid API key, or a file that is not a PDF invoice.
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }

    private static void flag(String area, BigDecimal score) {
        String note = score == null ? "no score" : score.compareTo(CHECK_BELOW) < 0 ? "check this closely" : "ok";
        System.out.println("  " + area + ": " + (score == null ? "-" : score.toPlainString()) + " (" + note + ")");
    }
}
