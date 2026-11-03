// Validates incoming invoices: a ZUGFeRD PDF and an XRechnung XML.
// A failed validation is a normal result, not an exception: you get the verdict and every
// finding with the rule, the message and the field it concerns. The example also saves the
// printable validation report PDF for the ZUGFeRD invoice.

import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.ReportOptions;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import com.invoicexml.zugferdxrechnung.validation.ValidationReport;
import com.invoicexml.zugferdxrechnung.validation.ValidationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class ValidateInvoices {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String ZUGFERD_PDF_PATH = "invoices/incoming-zugferd.pdf";
    private static final String XRECHNUNG_XML_PATH = "invoices/incoming-xrechnung.xml";
    private static final String REPORT_PATH = "incoming-zugferd-report.pdf";

    public static void main(String[] args) throws IOException {
        // Read both files first, so a wrong path stops the example before any API call.
        FileUpload zugferdPdf;
        FileUpload xrechnungXml;
        try {
            Path zugferdPdfPath = Path.of(ZUGFERD_PDF_PATH);
            zugferdPdf = FileUpload.of(zugferdPdfPath.getFileName().toString(), Files.readAllBytes(zugferdPdfPath));
            Path xrechnungXmlPath = Path.of(XRECHNUNG_XML_PATH);
            xrechnungXml = FileUpload.of(xrechnungXmlPath.getFileName().toString(), Files.readAllBytes(xrechnungXmlPath));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + e.getFile() + ".");
            System.out.println("Set ZUGFERD_PDF_PATH and XRECHNUNG_XML_PATH at the top of ValidateInvoices.java to files on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            // 1. Validate a ZUGFeRD PDF against the rules of the profile it declares.
            ValidationResult zugferd = client.validateZugferd(zugferdPdf);
            print("ZUGFeRD PDF", zugferd);

            // 2. Validate an XRechnung XML, UBL or CII, against EN 16931 and the German XRechnung rules.
            ValidationResult xrechnung = client.validateXRechnung(xrechnungXml);
            print("XRechnung XML", xrechnung);

            // 3. The same check as a printable PDF report, for example to send back to a supplier.
            ValidationReport report = client.validateZugferdReport(zugferdPdf,
                    new ReportOptions().setValidationLanguage(PdfLanguage.DE));
            Files.write(Path.of(REPORT_PATH), report.getFile());
            System.out.println("Saved the validation report to " + REPORT_PATH + " (valid: " + report.getValid() + ").");
        } catch (InvoiceXmlApiException e) {
            // For example an invalid API key, or a file that is not an invoice at all.
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }

    private static void print(String label, ValidationResult result) {
        System.out.println(label + ": " + (result.isValid() ? "valid" : "not valid") + ". " + result.getDetail());

        for (ValidationFinding error : result.getErrors()) {
            System.out.println("  error   [" + error.getRule() + "] " + error.getMessage()
                    + " (" + String.join(", ", error.getFields()) + ")");
        }

        // Warnings never make an invoice invalid, but are worth reviewing.
        for (ValidationFinding warning : result.getWarnings()) {
            System.out.println("  warning [" + warning.getRule() + "] " + warning.getMessage()
                    + " (" + String.join(", ", warning.getFields()) + ")");
        }
    }
}
