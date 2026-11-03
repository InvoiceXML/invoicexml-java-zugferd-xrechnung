// Converts a UBL invoice XML, for example one received over Peppol, into a ZUGFeRD invoice:
// a readable PDF with the invoice data embedded as CII XML.

import com.invoicexml.zugferdxrechnung.ConvertToPdfOptions;
import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceValidationException;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import com.invoicexml.zugferdxrechnung.validation.ValidationFinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class ConvertUblToZugferd {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String UBL_XML_PATH = "invoices/RE-2026-001-ubl.xml";
    private static final String OUTPUT_PATH = "RE-2026-001-zugferd.pdf";

    public static void main(String[] args) throws IOException {
        // Read the XML first, so a wrong path stops the example before any API call.
        FileUpload ubl;
        try {
            Path path = Path.of(UBL_XML_PATH);
            ubl = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + UBL_XML_PATH + ".");
            System.out.println("Set UBL_XML_PATH at the top of ConvertUblToZugferd.java to a UBL invoice XML on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            // The language sets the PDF layout and the default texts added when the UBL has none.
            byte[] zugferd = client.convertUblToZugferd(ubl,
                    new ConvertToPdfOptions().setLanguage(PdfLanguage.DE));

            Files.write(Path.of(OUTPUT_PATH), zugferd);
            System.out.println("Saved the ZUGFeRD invoice to " + OUTPUT_PATH + ".");
        } catch (InvoiceValidationException e) {
            // The result would not be a compliant ZUGFeRD invoice. Each finding names the field to fix.
            System.out.println("The converted invoice would not be compliant:");
            for (ValidationFinding finding : e.getErrors()) {
                System.out.println("  [" + finding.getRule() + "] " + finding.getMessage()
                        + " (" + String.join(", ", finding.getFields()) + ")");
            }
        } catch (InvoiceXmlApiException e) {
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }
}
