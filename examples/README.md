# Examples

Small console programs, one per common task. Each one is a complete Java file you can read from top to bottom and adapt.

| Example | What it shows |
|---|---|
| [CreateZugferdWithOwnPdf](CreateZugferdWithOwnPdf/CreateZugferdWithOwnPdf.java) | Create a ZUGFeRD invoice that keeps your own PDF layout, passed as a public URL (`setPdfUrl`). |
| [CreateCiiAndEmbed](CreateCiiAndEmbed/CreateCiiAndEmbed.java) | Build a CII XML from your invoice data with `createCii`, then embed it into a PDF you already have with `embedZugferd`. |
| [EmailZugferd](EmailZugferd/EmailZugferd.java) | Create a ZUGFeRD invoice and email it from your own verified domain in one call. |
| [EmailEmbeddedZugferd](EmailEmbeddedZugferd/EmailEmbeddedZugferd.java) | Build a CII XML from your invoice data, then embed it into a PDF you already have and email the ZUGFeRD invoice with `emailEmbeddedZugferd`. |
| [ValidateInvoices](ValidateInvoices/ValidateInvoices.java) | Validate a ZUGFeRD PDF and an XRechnung XML, print every finding, and save the printable validation report. |
| [RenderXmlToPdf](RenderXmlToPdf/RenderXmlToPdf.java) | Turn any invoice XML (XRechnung, CII or UBL) into a readable PDF. |
| [ConvertUblToZugferd](ConvertUblToZugferd/ConvertUblToZugferd.java) | Convert a UBL invoice XML into a ZUGFeRD invoice. |
| [TransformToZugferd](TransformToZugferd/TransformToZugferd.java) | Turn an ordinary PDF invoice into a ZUGFeRD invoice. AI reads the PDF, so a person checks the result. |
| [ParsePdfInvoice](ParsePdfInvoice/ParsePdfInvoice.java) | Read an ordinary PDF invoice, typed, scanned or photographed, into the invoice model with AI, and see how sure the read was per area. |
| [ExtractJsonAndAttachments](ExtractJsonAndAttachments/ExtractJsonAndAttachments.java) | Read a received ZUGFeRD invoice as a typed `InvoiceDocument` and save its embedded attachments as a ZIP. |

## Run an example

1. Open the example's Java file and replace the values at the top: `YOUR_API_KEY` with your API key, and the file paths or URLs with your own. Relative paths start from the repository root. An example whose file is missing tells you which value to change.
2. Run it from the repository root, for example:

```bash
./mvnw -q test-compile exec:java -Dexec.mainClass=CreateCiiAndEmbed
```

The examples compile against the library source in this repository, and every build compiles them, so they always match the code next to them. To use one in your own project, copy its Java file and add the `com.invoicexml:invoicexml-zugferd-xrechnung` dependency instead, as shown under [Installation](../README.md#installation).

Get an API key with the **30-day free trial: 100 credits, no credit card required** at [www.invoicexml.com/account/signup](https://www.invoicexml.com/account/signup). The calls use credits from your account, so run the examples with test data.
