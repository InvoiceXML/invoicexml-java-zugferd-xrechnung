# Changelog

## 0.1.1

- Link the website at https://www.invoicexml.com in the package metadata (the Maven Project URL now points to the website), the README and error messages.

## 0.1.0

First release.

- `ZugferdXRechnungClient` with create, validate, embed, render and extract calls for ZUGFeRD and XRechnung.
- EN 16931 invoice model generated from the InvoiceXML OpenAPI schema.
- Typed validation results and rule findings on rejected create requests.
- Automatic retry on HTTP 429 with `Retry-After` support.
