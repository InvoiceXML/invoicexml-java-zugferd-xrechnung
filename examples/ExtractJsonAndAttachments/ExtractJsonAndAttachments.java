// Reads a ZUGFeRD invoice you received: the invoice data as a typed InvoiceDocument, and the
// supporting documents embedded in it (timesheets, delivery notes) as one ZIP archive.

import com.invoicexml.zugferdxrechnung.ErrorCodes;
import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.InvoiceDocument;
import com.invoicexml.zugferdxrechnung.model.InvoiceLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ExtractJsonAndAttachments {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String ZUGFERD_PDF_PATH = "invoices/incoming-zugferd.pdf"; // an XRechnung XML works too
    private static final String ATTACHMENTS_ZIP_PATH = "incoming-zugferd-attachments.zip";

    public static void main(String[] args) throws IOException {
        // Read the file first, so a wrong path stops the example before any API call.
        FileUpload file;
        try {
            Path path = Path.of(ZUGFERD_PDF_PATH);
            file = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + ZUGFERD_PDF_PATH + ".");
            System.out.println("Set ZUGFERD_PDF_PATH at the top of ExtractJsonAndAttachments.java to a ZUGFeRD PDF on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            // 1. The invoice data, read from the embedded XML. It can be passed straight back to the create methods.
            InvoiceDocument invoice = client.extractJson(file);
            List<InvoiceLine> lines = invoice.getLines() == null ? List.of() : invoice.getLines();

            System.out.println("Invoice " + invoice.getInvoiceNumber()
                    + " from " + (invoice.getSeller() == null ? null : invoice.getSeller().getName())
                    + " to " + (invoice.getBuyer() == null ? null : invoice.getBuyer().getName()));
            if (invoice.getTotals() != null) {
                System.out.println("Total " + invoice.getTotals().getGrandTotalAmount() + " " + invoice.getCurrency()
                        + ", due " + invoice.getTotals().getDuePayableAmount() + ", " + lines.size() + " line(s)");
            }
            for (InvoiceLine line : lines) {
                System.out.println("  " + line.getQuantity() + " x "
                        + (line.getItem() == null ? null : line.getItem().getName())
                        + " at " + (line.getPriceDetails() == null ? null : line.getPriceDetails().getNetPrice()));
            }

            // 2. The embedded supporting documents, as one ZIP archive.
            try {
                byte[] attachments = client.extractAttachments(file);
                Files.write(Path.of(ATTACHMENTS_ZIP_PATH), attachments);
                System.out.println("Saved the attachments to " + ATTACHMENTS_ZIP_PATH + ".");
            } catch (InvoiceXmlApiException e) {
                if (!Objects.equals(e.getErrorCode(), ErrorCodes.NO_ATTACHMENTS)) {
                    throw e;
                }
                System.out.println("The invoice has no embedded attachments.");
            }
        } catch (InvoiceXmlApiException e) {
            // For example an invalid API key, or a PDF without an embedded invoice.
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }
}
