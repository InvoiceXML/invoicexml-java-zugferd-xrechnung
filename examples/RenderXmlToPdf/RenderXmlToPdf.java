// Turns any invoice XML into a readable PDF, for approval workflows and archives.
// XRechnung, CII (such as the XML inside a ZUGFeRD invoice) and UBL are all accepted;
// the format is detected from the XML.

import com.invoicexml.zugferdxrechnung.FileUpload;
import com.invoicexml.zugferdxrechnung.InvoiceXmlApiException;
import com.invoicexml.zugferdxrechnung.RenderOptions;
import com.invoicexml.zugferdxrechnung.ZugferdXRechnungClient;
import com.invoicexml.zugferdxrechnung.model.PdfLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class RenderXmlToPdf {

    // Replace these values with your own.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String XML_PATH = "invoices/RE-2026-001.xml";
    private static final String OUTPUT_PATH = "RE-2026-001-preview.pdf";

    public static void main(String[] args) throws IOException {
        // Read the XML first, so a wrong path stops the example before any API call.
        FileUpload xml;
        try {
            Path path = Path.of(XML_PATH);
            xml = FileUpload.of(path.getFileName().toString(), Files.readAllBytes(path));
        } catch (NoSuchFileException e) {
            System.out.println("Could not find " + XML_PATH + ".");
            System.out.println("Set XML_PATH at the top of RenderXmlToPdf.java to an invoice XML on your machine.");
            return;
        }

        ZugferdXRechnungClient client = ZugferdXRechnungClient.create(API_KEY);

        try {
            RenderOptions options = new RenderOptions()
                    .setLanguage(PdfLanguage.DE)   // labels and date formats of the PDF
                    .setIncludeQrCode(true);       // optional: a payment QR code for banking apps
            // options.setLogoUrl("https://example.com/logo.png"); // optional: public URL of your logo, printed top left

            byte[] pdf = client.renderXmlToPdf(xml, options);
            Files.write(Path.of(OUTPUT_PATH), pdf);
            System.out.println("Saved the PDF to " + OUTPUT_PATH + ".");
        } catch (InvoiceXmlApiException e) {
            // For example an invalid API key, or a file that is not an invoice XML.
            System.out.println("The API refused the request: " + e.getMessage());
        }
    }
}
