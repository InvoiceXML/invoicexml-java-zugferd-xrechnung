# InvoiceXML ZUGFeRD and XRechnung client for Java

Create, validate, embed, render and extract **ZUGFeRD** and **XRechnung** e-invoices from Java with the [InvoiceXML API](https://invoicexml.com). Build the invoice from typed EN 16931 models and get back a compliant ZUGFeRD PDF/A-3 or XRechnung XML (UBL or CII), already validated against the official rule set.

- Java 11 or later, `java.net.http.HttpClient`, Jackson as the only dependency
- Typed EN 16931 invoice model generated from the API's OpenAPI schema
- Rejected invoices surface the violated rules as typed findings
- Automatic retry on HTTP 429, honouring `Retry-After`

## Installation

Maven:

```xml
<dependency>
    <groupId>com.invoicexml</groupId>
    <artifactId>invoicexml-zugferd-xrechnung</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.invoicexml:invoicexml-zugferd-xrechnung:0.1.0")
```

Gradle (Groovy DSL):

```groovy
implementation 'com.invoicexml:invoicexml-zugferd-xrechnung:0.1.0'
```

## Get an API key

Sign up at [invoicexml.com/account/signup](https://invoicexml.com/account/signup) for a 30-day free trial with 100 credits. No credit card is needed.

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
| `embedZugferd(pdf, xml[, options])` | `POST /v1/embed/zugferd` | ZUGFeRD PDF/A-3 (`byte[]`) |
| `renderXRechnungToPdf(xml[, options])` | `POST /v1/render/xrechnung/to/pdf` | Readable PDF (`byte[]`) |
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

## Errors, rate limits and retries

All exceptions are unchecked and extend `InvoiceXmlException`.

| Exception | When |
|---|---|
| `InvoiceValidationException` | HTTP 400 with rule findings |
| `AuthenticationException` | HTTP 401, the API key is missing or inactive |
| `PayloadTooLargeException` | HTTP 413, a file exceeds 20 MB |
| `RateLimitException` | HTTP 429 after the automatic retries |
| `InvoiceXmlApiException` | Any other error response, with `getErrorCode()` from `ErrorCodes` |
| `InvoiceXmlConnectionException` | The API could not be reached |

On HTTP 429 the client waits for the `Retry-After` delay and retries, three times by default. Rate-limited requests consume no credits.

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
- **Zero data retention.** Invoices are processed in memory and purged when the response is delivered. Nothing is stored or logged, and nothing is used for AI training.
- **Processing in Frankfurt.** All processing happens in the EU, in Frankfurt, Germany. See [invoicexml.com/trust](https://invoicexml.com/trust).
- **Integration assistance.** Questions about your integration go to [support@invoicexml.com](mailto:support@invoicexml.com), from setup to your first production invoice.

## Documentation

- API reference: [api.invoicexml.com/v1/scalar](https://api.invoicexml.com/v1/scalar/)
- OpenAPI 3.1 schema: [api.invoicexml.com/v1/openapi](https://api.invoicexml.com/v1/openapi)
- Factur-X: see the companion library [invoicexml-java-facturx](https://github.com/InvoiceXML/invoicexml-java-facturx)

## Development

```bash
./mvnw verify
```

The classes in `com.invoicexml.zugferdxrechnung.model` are generated from the OpenAPI schema. To refresh them after a schema change, run `node tools/generate-models.mjs --fetch` (Node 18 or later) and commit the result.

## License

MIT. See [LICENSE](LICENSE).
