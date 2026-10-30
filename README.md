# InvoiceXML ZUGFeRD and XRechnung client for Java

Create, validate, embed, render, convert and extract **ZUGFeRD** and **XRechnung** e-invoices from Java with the [InvoiceXML API](https://www.invoicexml.com). Build the invoice from typed EN 16931 models and get back a compliant ZUGFeRD PDF/A-3 or XRechnung XML (UBL or CII), already validated against the official rule set.

- Java 11 or later, `java.net.http.HttpClient`, Jackson as the only dependency
- Typed EN 16931 invoice model generated from the API's OpenAPI schema
- Rejected invoices surface the violated rules as typed findings
- Automatic retry on HTTP 429 on every call except transform, honouring `Retry-After`

## Installation

Maven:

```xml
<dependency>
    <groupId>com.invoicexml</groupId>
    <artifactId>invoicexml-zugferd-xrechnung</artifactId>
    <version>0.3.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.invoicexml:invoicexml-zugferd-xrechnung:0.3.0")
```

Gradle (Groovy DSL):

```groovy
implementation 'com.invoicexml:invoicexml-zugferd-xrechnung:0.3.0'
```

## Get an API key

Sign up at [www.invoicexml.com/account/signup](https://www.invoicexml.com/account/signup) for a 30-day free trial with 100 credits. No credit card is needed.

## Create a ZUGFeRD invoice

```java
var client = ZugferdXRechnungClient.create(System.getenv("INVOICEXML_API_KEY"));

var invoice = new InvoiceDocument()
    .setInvoiceNumber("RE-2026-001").setIssueDate(LocalDate.of(2026, 7, 25)).setCurrency("EUR")
    .setSeller(new SellerParty().setName("Mustermann Software GmbH").setVatIdentifier("DE123456789")
        .setPostalAddress(new SellerPostalAddress().setLine1("Hauptstrasse 12").setCity("Berlin").setPostCode("10115").setCountry("DE")))
    .setBuyer(new BuyerParty().setName("Beispiel Handel AG")
        .setPostalAddress(new BuyerPostalAddress().setLine1("Marienplatz 8").setCity("Muenchen").setPostCode("80331").setCountry("DE")))
    .setPaymentDetails(new PaymentDetails().setPaymentAccountIdentifier("DE89370400440532013000"))
    .addLine(new InvoiceLine().setQuantity(new BigDecimal("10")).setItem(new ItemInformation().setName("Softwareentwicklung"))
        .setPriceDetails(new PriceDetails().setNetPrice(new BigDecimal("250.00"))).setVatInformation(new LineVatInformation().setRate(new BigDecimal("19"))));

Files.write(Path.of("invoice-zugferd.pdf"), client.createZugferd(invoice));
```

Totals and the VAT breakdown are calculated from the lines. The PDF is a ZUGFeRD 2.x PDF/A-3 with the EN 16931 profile and the CII XML embedded.

## API surface

| Method | Endpoint | Returns |
|---|---|---|
| `createZugferd(invoice[, options])` | `POST /v1/create/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `createXRechnung(invoice[, options])` | `POST /v1/create/xrechnung` | XRechnung XML (`String`) |
| `validateZugferd(pdf)` | `POST /v1/validate/zugferd` | `ValidationResult` |
| `validateXRechnung(xml)` | `POST /v1/validate/xrechnung` | `ValidationResult` |
| `validateZugferdReport(pdf[, options])` | `POST /v1/validate/zugferd/report` | `ValidationReport` |
| `validateXRechnungReport(xml[, options])` | `POST /v1/validate/xrechnung/report` | `ValidationReport` |
| `embedZugferd(pdf, xml[, options])` | `POST /v1/embed/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `emailZugferd(invoice, email[, options])` | `POST /v1/create/zugferd/email` | `EmailSendResult` |
| `emailXRechnung(invoice, email[, options])` | `POST /v1/create/xrechnung/email` | `EmailSendResult` |
| `emailEmbeddedZugferd(pdf, xml, email)` | `POST /v1/embed/zugferd/email` | `EmailSendResult` |
| `renderXRechnungToPdf(xml[, options])` | `POST /v1/render/xrechnung/to/pdf` | Readable PDF (`byte[]`) |
| `renderCiiToPdf(xml[, options])` | `POST /v1/render/cii/to/pdf` | Readable PDF (`byte[]`) |
| `convertZugferdToUbl(pdf[, options])` | `POST /v1/convert/zugferd/to/ubl` | UBL XML (`String`) |
| `convertZugferdToCii(pdf[, options])` | `POST /v1/convert/zugferd/to/cii` | CII XML (`String`) |
| `convertXRechnungToUbl(xml[, options])` | `POST /v1/convert/xrechnung/to/ubl` | UBL XML (`String`) |
| `convertXRechnungToCii(xml[, options])` | `POST /v1/convert/xrechnung/to/cii` | CII XML (`String`) |
| `convertUblToXRechnung(xml[, options])` | `POST /v1/convert/ubl/to/xrechnung` | XRechnung XML (`String`) |
| `convertCiiToXRechnung(xml[, options])` | `POST /v1/convert/cii/to/xrechnung` | XRechnung XML (`String`) |
| `convertCiiToZugferd(xml[, options])` | `POST /v1/convert/cii/to/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `convertUblToZugferd(xml[, options])` | `POST /v1/convert/ubl/to/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `convertXRechnungToZugferd(xml[, options])` | `POST /v1/convert/xrechnung/to/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `transformToZugferd(pdf[, options])` | `POST /v1/transform/to/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`), check before use |
| `transformToXRechnung(pdf, buyerReference[, options])` | `POST /v1/transform/to/xrechnung` | XRechnung XML (`String`), check before use |
| `extractJson(file)` | `POST /v1/extract/json` | `InvoiceDocument` |
| `extractAttachments(file)` | `POST /v1/extract/attachments` | ZIP of embedded documents (`byte[]`) |

File parameters accept a `Path` or a `FileUpload`. `FileUpload.ofPdf(bytes)` and `FileUpload.ofXml(string)` cover in-memory documents.

## Create an XRechnung

XRechnung adds the German CIUS rules on top of EN 16931. The Leitweg-ID goes in `buyerReference`, and seller contact and electronic addresses are required. UBL 2.1 is the default syntax. Pick CII through the options:

```java
String xml = client.createXRechnung(invoice, new InvoiceOptions().setSyntax(XRechnungSyntax.CII));
```

For ZUGFeRD, the options also set the PDF language, a logo, a brand colour, or your own PDF as the visual layer:

```java
byte[] pdf = client.createZugferd(invoice, new InvoiceOptions()
    .setLanguage(PdfLanguage.DE)
    .setLogoUrl("https://example.com/logo.png"));
```

## Send an invoice by email

The email calls create the same document as the create calls, then send it as a real email from your own address. The sender must be on a domain added and verified on the Verified email domains page of the dashboard:

```java
var email = EmailMessage.of("invoices@yourcompany.com", "ap@customer.example")
    .setFromName("Your Company Billing")
    .addCc("accounting@yourcompany.com");

EmailSendResult sent = client.emailZugferd(invoice, email);
System.out.println(sent.getMessageId());
```

`emailXRechnung(invoice, email)` sends an XRechnung the same way, and `emailEmbeddedZugferd(pdf, xml, email)` sends a ZUGFeRD PDF built from your own PDF and CII XML, always with full validation. The subject and message are optional, and a sensible default applies when you leave them out.

The result carries the message id, not the file. `accepted` means the email was handed over for delivery, not that the recipient's server has received it yet. Only HTTP 429 is retried on these calls, so a failed call never sends a second email by itself. After an `InvoiceXmlConnectionException` the email may or may not have gone out, so check before you send it again.

A refused send raises `InvoiceXmlApiException`. Branch on `getErrorCode()`: `ErrorCodes.EMAIL_DOMAIN_NOT_VERIFIED`, `ErrorCodes.EMAIL_SENDING_DISABLED`, or `ErrorCodes.EMAIL_NOT_IN_PLAN` when your plan does not include this (see [pricing](https://www.invoicexml.com/pricing)). A bad address raises `InvoiceValidationException` with the field in its findings. See the [email guide](https://www.invoicexml.com/docs/api/create/zugferd/email) for the details.

## Rejected invoices

Every create call validates the invoice before a document is produced. When rules are violated, the call throws `InvoiceValidationException` with the findings, so a non-compliant invoice never leaves your system:

```java
try {
    client.createXRechnung(invoice);
} catch (InvoiceValidationException e) {
    for (ValidationFinding f : e.getErrors()) {
        System.out.println(f.getRule() + " " + f.getFields() + ": " + f.getMessage());
    }
}
```

Each finding carries the rule id (for example `BR-DE-15`), the layer (`xsd`, `en16931` or `cius`), a plain-language message, the field paths into your invoice, and the verbatim validator output.

## Validate incoming invoices

Valid and invalid invoices both return a `ValidationResult`. Branch on `isValid()`:

```java
ValidationResult result = client.validateZugferd(Path.of("incoming.pdf"));
if (!result.isValid()) {
    result.getErrors().forEach(System.out::println);
}
```

The result also carries the detected profile, the schema and Schematron verdicts, and a field report with one row per invoice field.

## Validation reports

`validateZugferdReport` and `validateXRechnungReport` run the same validation and return a printable PDF report with the verdict. A failed verdict is a normal result, not an exception:

```java
ValidationReport report = client.validateZugferdReport(FileUpload.of(Path.of("incoming.pdf")),
    new ReportOptions().setValidationLanguage(PdfLanguage.DE));

Files.write(Path.of("report.pdf"), report.getFile());
if (Boolean.FALSE.equals(report.getValid())) {
    System.out.println("The invoice failed validation");
}
```

`getValid()` is `true`, `false`, or `null` when the response carries no verdict. A missing or malformed file still raises an exception. See the [ZUGFeRD report](https://www.invoicexml.com/docs/api/validate/zugferd/report) and [XRechnung report](https://www.invoicexml.com/docs/api/validate/xrechnung/report) guides.

## Render an invoice as PDF

`renderXRechnungToPdf` and `renderCiiToPdf` turn an invoice XML into a readable PDF:

```java
byte[] pdf = client.renderXRechnungToPdf(FileUpload.of(Path.of("rechnung.xml")), new RenderOptions()
    .setLanguage(PdfLanguage.DE)
    .setLogoUrl("https://example.com/logo.png")
    .setIncludeQrCode(true));
```

English is the default language, and the logo and the EPC payment QR code are optional. See the [XRechnung](https://www.invoicexml.com/docs/api/render/xrechnung/to/pdf) and [CII](https://www.invoicexml.com/docs/api/render/cii/to/pdf) guides.

## Convert between formats

Each conversion has its own method, named after its route. Conversions to XML return a `String`, conversions to ZUGFeRD return the PDF:

```java
String ubl = client.convertZugferdToUbl(Path.of("rechnung.pdf"));
String xrechnung = client.convertCiiToXRechnung(Path.of("rechnung.xml"));
byte[] pdf = client.convertCiiToZugferd(FileUpload.of(Path.of("rechnung.xml")),
    new ConvertToPdfOptions().setLanguage(PdfLanguage.DE));
```

The result is validated before it is returned, and a rejected conversion throws `InvoiceValidationException`. `setValidationLanguage` on `ConvertOptions` or `ConvertToPdfOptions` words its findings. See the [convert guide](https://www.invoicexml.com/docs/api/convert/zugferd/to/ubl) for the details.

## Transform a PDF invoice

`transformToZugferd` and `transformToXRechnung` build an e-invoice from a PDF invoice. AI reads the PDF, so a person must check the result before it is used.

```java
byte[] zugferd = client.transformToZugferd(FileUpload.of(Path.of("invoice.pdf")), new TransformOptions()
    .setLanguage(PdfLanguage.DE));
String xrechnung = client.transformToXRechnung(Path.of("invoice.pdf"), "04011000-12345-34");
```

Without a language, the PDF uses the language the invoice is written in. `setPreserveOriginalPdf(true)` keeps the uploaded pages and only embeds the XML. Otherwise the PDF is rendered fresh and ends with an Electronic invoice details section for checking the technical values, which `setIncludeAdvancedProperties(false)` leaves out. A 429 on these methods is not retried automatically, so wait for `Retry-After` before calling again if you want to retry. See the [ZUGFeRD](https://www.invoicexml.com/docs/api/transform/to/zugferd) and [XRechnung](https://www.invoicexml.com/docs/api/transform/to/xrechnung) guides.

## Errors, rate limits and retries

All exceptions are unchecked and extend `InvoiceXmlException`.

| Exception | When |
|---|---|
| `InvoiceValidationException` | HTTP 400 with rule findings |
| `AuthenticationException` | HTTP 401, the API key is missing or inactive |
| `PayloadTooLargeException` | HTTP 413, a file exceeds 20 MB |
| `RateLimitException` | HTTP 429 after any automatic retries |
| `InvoiceXmlApiException` | Any other error response, with `getErrorCode()` from `ErrorCodes` |
| `InvoiceXmlConnectionException` | The API could not be reached |

On HTTP 429 every call except transform waits for the `Retry-After` delay and retries, three times by default. A 429 is not billed.

```java
var client = ZugferdXRechnungClient.builder()
    .apiKey(apiKey)
    .maxRetries(5)
    .maxRetryWait(Duration.ofSeconds(30))
    .timeout(Duration.ofSeconds(60))
    .build();
```

The client is immutable and thread-safe. Create one instance and share it. To reuse your own `java.net.http.HttpClient` (proxy, executor, SSL context), pass `new JdkHttpTransport(httpClient)` to `transport(...)`.

## Why InvoiceXML

- **Rule sets updated on their legal effective dates.** New ZUGFeRD, XRechnung and EN 16931 rule versions go live on the API the day they take legal effect. The rules run on the server, so you never update this library or redeploy for a new rule version.
- **Validated before delivery.** Every document is checked against the official XSD and Schematron rules before it is returned.
- **Zero data retention.** Invoices are processed in memory and purged when the response is delivered. Nothing is stored or logged, and nothing is used for AI training. When you email an invoice, it goes to our email service for delivery, and we keep no copy.
- **Processing in Frankfurt.** All processing happens in the EU, in Frankfurt, Germany. See [www.invoicexml.com/trust](https://www.invoicexml.com/trust).
- **Integration assistance.** Questions about your integration go to [support@invoicexml.com](mailto:support@invoicexml.com), from setup to your first production invoice.

## Documentation

- API reference: [api.invoicexml.com/v1/scalar](https://api.invoicexml.com/v1/scalar/)
- OpenAPI 3.1 schema: [api.invoicexml.com/v1/openapi](https://api.invoicexml.com/v1/openapi)
- Factur-X: see the companion library [invoicexml-java-facturx](https://github.com/InvoiceXML/invoicexml-java-facturx)

## Development

```bash
./mvnw verify
```

The unit tests run against a stubbed HTTP layer. A live suite against the real API runs only when you opt in, and it consumes about 13 credits per run:

```bash
INVOICEXML_API_KEY=your-key ./mvnw verify -Dinvoicexml.live=true
```

The classes in `com.invoicexml.zugferdxrechnung.model` are generated from the OpenAPI schema. To refresh them after a schema change, run `node tools/generate-models.mjs --fetch` (Node 18 or later) and commit the result.

## License

MIT. See [LICENSE](LICENSE).
