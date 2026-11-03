// Turns an ordinary PDF invoice, for example one from an older billing system or a supplier,
// into a ZUGFeRD invoice. AI reads the PDF, so a person must check the result before it is used.
// The rendered PDF ends with a section of technical properties to make that check easy.

import com.invoicexml.zugferdxrechnung.ErrorCodes;
import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceValidationException;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.RateLimitException;
import com.invoicexml.zugferdxrechnung.TransformOptions;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

public class TransformToZugferd {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String PDF_PATH = "invoices/RE-2026-001.pdf"; // an ordinary PDF invoice
    private static final String OUTPUT_PATH = "RE-2026-001-zugferd.pdf";

    public static void main(String[] args) throws IOException {
        // Read the PDF first, so a wrong path stops the example before any API call.
        FileUpload pdf;
        try {
            Path path = Path.of(PDF_PATH);
            pdf = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + PDF_PATH + ".");
            System.out.println("Set PDF_PATH at the top of TransformToZugferd.java to a PDF invoice on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            TransformOptions options = new TransformOptions();
            // options.setLanguage(PdfLanguage.DE);           // optional: left empty, the language the invoice is written in is used
            // options.setPreserveOriginalPdf(true);          // optional: keep your PDF as the visual layer instead of a fresh layout
            // options.setIncludeAdvancedProperties(false);   // optional: leave out the technical properties section

            byte[] zugferd = client.transformToZugferd(pdf, options);

            Files.write(Path.of(OUTPUT_PATH), zugferd);
            System.out.println("Saved the ZUGFeRD invoice to " + OUTPUT_PATH + ". Check it before you send or book it.");
        } catch (RateLimitException e) {
            // Transform calls are not retried automatically, because a retry repeats the AI read.
            System.out.println(e.getRetryAfter() != null
                    ? "Too many requests. Wait " + e.getRetryAfter().getSeconds() + " seconds before trying again."
                    : "Too many requests. Wait a moment before trying again.");
        } catch (InvoiceXmlApiException e) {
            if (Objects.equals(e.getErrorCode(), ErrorCodes.TOTALS_MISMATCH)) {
                // The amounts read from the PDF do not add up to its printed total, so no invoice was created.
                System.out.println("The lines, allowances, charges and VAT read from the PDF do not add up to its printed total.");
                System.out.println("Check the PDF, or create the invoice from your own data with createZugferd instead.");
            } else if (e instanceof InvoiceValidationException) {
                // The data read from the PDF would not make a compliant ZUGFeRD invoice.
                System.out.println("The invoice read from the PDF is not compliant:");
                for (ValidationFinding finding : ((InvoiceValidationException) e).getErrors()) {
                    System.out.println("  [" + finding.getRule() + "] " + finding.getMessage()
                            + " (" + String.join(", ", finding.getFields()) + ")");
                }
            } else {
                // For example an invalid API key, or a file that is not a PDF invoice.
                System.out.println("The API refused the request: " + e.getMessage());
            }
        }
    }
}
