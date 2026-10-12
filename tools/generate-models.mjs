#!/usr/bin/env node
// Generates the Java invoice model classes from the InvoiceXML OpenAPI schema.
//
//   node tools/generate-models.mjs            (uses tools/openapi.json)
//   node tools/generate-models.mjs --fetch    (downloads the live schema first)
//
// Field names, nesting, required flags and number/date formats come from the
// schema. Field descriptions come from tools/field-docs.json and, for the
// options, from tools/generator-config.json.
//
// The published schema describes a few properties less precisely than the API
// serializes them. These wire-format corrections are applied here, in one place:
//   * CountryCode, CurrencyCode, VatCategoryCode, InvoiceTypeCode are documented
//     as integers but travel as string codes ("DE", "EUR", "S", "380").
//   * PdfLanguage is documented as an integer but travels as "EN", "DE" or "FR".
//   * PaymentDetails.paymentMeansCode is untyped; it is a UNTDID 4461 string code.
//   * InvoiceDocument.notes is an untyped array; each entry is an InvoiceNote
//     object ({ note, subjectCode }), and the API also accepts a bare string.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '..');
const specPath = path.join(here, 'openapi.json');

if (process.argv.includes('--fetch')) {
  const res = await fetch('https://api.invoicexml.com/v1/openapi');
  if (!res.ok) throw new Error(`Schema download failed: HTTP ${res.status}`);
  fs.writeFileSync(specPath, await res.text());
}

const config = JSON.parse(fs.readFileSync(path.join(here, 'generator-config.json'), 'utf8'));
const spec = pruneSpec(JSON.parse(fs.readFileSync(specPath, 'utf8')));
// The committed snapshot holds only the schemas this library uses, without
// descriptions, so it is exactly the generator's input.
fs.writeFileSync(specPath, JSON.stringify(spec, null, 2) + '\n');

function pruneSpec(full) {
  const all = full.components.schemas;
  const keep = new Set();
  const walk = (name) => {
    if (keep.has(name) || !all[name]) return;
    keep.add(name);
    for (const m of JSON.stringify(all[name]).matchAll(/#\/components\/schemas\/(\w+)/g)) walk(m[1]);
  };
  config.roots.forEach(walk);
  // Drops "description" annotations, but never a property that happens to be
  // named "description" (keys of a "properties" map are field names).
  const strip = (v) => {
    if (Array.isArray(v)) return v.map(strip);
    if (v && typeof v === 'object') {
      const o = {};
      for (const [k, x] of Object.entries(v)) {
        if (k === 'description' && typeof x === 'string') continue;
        if (k === 'properties' && x && typeof x === 'object') {
          o[k] = Object.fromEntries(Object.entries(x).map(([p, d]) => [p, strip(d)]));
        } else {
          o[k] = strip(x);
        }
      }
      return o;
    }
    return v;
  };
  const schemas = {};
  for (const n of [...keep].sort()) schemas[n] = strip(all[n]);
  return { openapi: full.openapi, info: { title: full.info.title, version: full.info.version }, components: { schemas } };
}
const fieldDocs = JSON.parse(fs.readFileSync(path.join(here, 'field-docs.json'), 'utf8'));
const schemas = structuredClone(spec.components.schemas);

// ---- wire-format corrections (see header) --------------------------------
const STRING_CODES = {
  CountryCode: 'ISO 3166-1 alpha-2 country code, for example {@code "DE"}.',
  CurrencyCode: 'ISO 4217 currency code, for example {@code "EUR"}.',
  VatCategoryCode: 'UNTDID 5305 VAT category code, for example {@code "S"} (standard rate). See {@link VatCategoryCodes}.',
  InvoiceTypeCode: 'UNTDID 1001 document type code, for example {@code "380"} (commercial invoice). See {@link InvoiceTypeCodes}.',
};
schemas.PaymentDetails.properties.paymentMeansCode = { type: ['null', 'string'] };
schemas.InvoiceDocument.properties.notes = { type: 'array', items: { $ref: '#/components/schemas/InvoiceNote' } };
schemas.InvoiceNote = {
  type: 'object',
  required: ['note'],
  properties: { note: { type: ['null', 'string'] }, subjectCode: { type: ['null', 'string'] } },
};

// ---- class-level documentation -------------------------------------------
const CLASS_DOCS = {
  CreateInvoiceRequest: 'Request body of the create endpoints: the invoice plus optional processing options.',
  InvoiceDocument: 'An EN 16931 invoice, the shared document model of every create and extract endpoint.',
  InvoiceNote: 'Invoice note: free text plus an optional UNTDID 4451 subject code.',
  InvoiceOptions: 'Optional rendering and processing settings. Defaults apply when omitted.',
  SellerParty: 'The seller.',
  SellerPostalAddress: 'Seller postal address.',
  SellerContact: 'Seller contact.',
  SellerIdentifier: 'Seller identifier with its optional scheme identifier.',
  SellerLegalRegistration: 'Seller legal registration identifier with its optional scheme identifier.',
  SellerElectronicAddress: 'Seller electronic address with its scheme identifier.',
  BuyerParty: 'The buyer.',
  BuyerPostalAddress: 'Buyer postal address.',
  BuyerContact: 'Buyer contact.',
  BuyerIdentifier: 'Buyer identifier with its optional scheme identifier.',
  BuyerLegalRegistration: 'Buyer legal registration identifier with its optional scheme identifier.',
  BuyerElectronicAddress: 'Buyer electronic address with its scheme identifier.',
  PrecedingInvoiceReference: 'Reference to a preceding invoice.',
  DeliveryInformation: 'Delivery information.',
  DeliveryPostalAddress: 'Deliver-to address.',
  InvoicingPeriod: 'Invoicing period.',
  PaymentDetails: 'Payment instructions and payment terms.',
  DocumentLevelAllowance: 'Document level allowance.',
  DocumentLevelCharge: 'Document level charge.',
  DocumentTotals: 'Document totals. Calculated from the lines when omitted.',
  VatBreakdown: 'VAT breakdown. Calculated from the lines when omitted.',
  AdditionalSupportingDocument: 'Additional supporting document, referenced or embedded.',
  InvoiceLine: 'Invoice line.',
  InvoiceLinePeriod: 'Invoice line period.',
  LineAllowance: 'Invoice line allowance.',
  LineCharge: 'Invoice line charge.',
  PriceDetails: 'Price details.',
  LineVatInformation: 'Line VAT information.',
  ItemInformation: 'Item information.',
  ItemAttribute: 'Item attribute: a name and a value.',
  ItemStandardIdentifier: 'Item standard identifier with its scheme identifier, for example a GTIN.',
  ItemClassification: 'Item classification identifier with scheme and scheme version.',
  LineObjectIdentifier: 'Invoice line object identifier with its scheme identifier.',
};

// ---- helpers --------------------------------------------------------------
const refName = (r) => r.split('/').pop();
const cap = (s) => s[0].toUpperCase() + s.slice(1);
const singular = (s) => (s.endsWith('s') ? s.slice(0, -1) : s);
const JAVA_KEYWORDS = new Set(['default', 'class', 'package', 'import', 'new', 'final', 'public']);
const field = (p) => (JAVA_KEYWORDS.has(p) ? p + 'Value' : p);
const escapeDoc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\*\//g, '*&#47;');

function unwrapRef(prop) {
  if (prop.$ref) return refName(prop.$ref);
  for (const k of ['oneOf', 'anyOf', 'allOf']) {
    const r = (prop[k] || []).find((x) => x.$ref);
    if (r) return refName(r.$ref);
  }
  return null;
}

/** Returns { java, imports, ref, list, enumOf } for a schema property. */
function javaType(owner, name, prop) {
  const enumCfg = config.stringEnums?.[`${owner}.${name}`];
  if (enumCfg) {
    const item = { java: enumCfg.type, imports: [], enumOf: enumCfg };
    return [].concat(prop.type || []).includes('array')
      ? { java: `List<${enumCfg.type}>`, imports: ['java.util.List', 'java.util.ArrayList'], list: item }
      : item;
  }
  const ref = unwrapRef(prop);
  if (ref) {
    if (STRING_CODES[ref]) return { java: 'String', imports: [], code: ref };
    if (ref === 'PdfLanguage') return { java: 'PdfLanguage', imports: [] };
    return { java: ref, imports: [], ref };
  }
  const types = [].concat(prop.type || []).filter((t) => t !== 'null');
  if (types.includes('array')) {
    const item = javaType(owner, name, prop.items || { type: 'string' });
    return { java: `List<${item.java}>`, imports: ['java.util.List', 'java.util.ArrayList', ...item.imports], ref: item.ref, list: item };
  }
  if (types.includes('number')) return { java: 'BigDecimal', imports: ['java.math.BigDecimal'] };
  if (types.includes('integer')) return { java: 'Integer', imports: [] };
  if (types.includes('boolean')) return { java: 'Boolean', imports: [] };
  if (types.includes('string')) {
    if (prop.format === 'date') return { java: 'LocalDate', imports: ['java.time.LocalDate'] };
    if (prop.format === 'byte') return { java: 'byte[]', imports: [] };
    return { java: 'String', imports: [] };
  }
  throw new Error(`Unmapped type for ${owner}.${name}: ${JSON.stringify(prop)}`);
}

function propertyDoc(owner, name, prop, type) {
  const parts = [];
  const doc = config.propertyDocs?.[`${owner}.${name}`] ?? fieldDocs[owner]?.[name];
  if (doc) parts.push(doc.includes('{@') ? doc : escapeDoc(doc));
  if (type.code) parts.push(STRING_CODES[type.code]);
  if (type.java === 'byte[]') parts.push('Sent and received as base64 in JSON.');
  return parts.join(' ');
}

// ---- collect the reachable schemas ---------------------------------------
const reachable = new Set();
(function visit(names) {
  for (const n of names) {
    if (reachable.has(n) || STRING_CODES[n] || n === 'PdfLanguage') continue;
    const s = schemas[n];
    if (!s) throw new Error(`Unknown schema ${n}`);
    reachable.add(n);
    const next = [];
    for (const [p, def] of Object.entries(s.properties || {})) {
      if ((config.excludeProperties?.[n] || []).includes(p)) continue;
      const t = javaType(n, p, def);
      if (t.ref) next.push(t.ref);
    }
    visit(next);
  }
})(config.roots);

// ---- emit -------------------------------------------------------------------
const outDir = path.join(repo, config.outDir);
fs.mkdirSync(outDir, { recursive: true });
for (const f of fs.readdirSync(outDir)) if (f.endsWith('.java')) fs.rmSync(path.join(outDir, f));

const HEADER = '// Generated by tools/generate-models.mjs from the InvoiceXML OpenAPI schema. Do not edit by hand.\n';
const pkg = config.package;
const written = [];

function emitClass(name) {
  const s = schemas[name];
  // Identifier records (identifier + scheme) list every member as required in
  // the schema, but only the non-nullable members must be sent.
  const isRecord = !!s.properties?.identifier || name === 'ItemAttribute';
  const required = new Set((s.required || []).filter((r) =>
    !isRecord || ![].concat(s.properties[r]?.type || []).includes('null')));
  const props = Object.entries(s.properties || {}).filter(([p]) => !(config.excludeProperties?.[name] || []).includes(p));
  const imports = new Set([
    'com.fasterxml.jackson.annotation.JsonAutoDetect',
    'com.fasterxml.jackson.annotation.JsonIgnoreProperties',
    'com.fasterxml.jackson.annotation.JsonInclude',
    'com.fasterxml.jackson.annotation.JsonProperty',
    'java.util.Objects',
  ]);
  const fields = props.map(([p, def]) => {
    const type = javaType(name, p, def);
    type.imports.forEach((i) => imports.add(i));
    return { json: p, name: field(p), type, required: required.has(p), doc: propertyDoc(name, p, def, type) };
  });
  if (fields.some((f) => f.type.java === 'byte[]')) imports.add('java.util.Arrays');
  const isIdentifierPair = fields.length >= 2 && fields[0].json === 'identifier' && fields[1].json === 'schemeId';
  const isNote = name === 'InvoiceNote';
  if (isNote) imports.add('com.fasterxml.jackson.annotation.JsonCreator');

  let o = HEADER + `package ${pkg};\n\n`;
  o += [...imports].sort().map((i) => `import ${i};\n`).join('') + '\n';
  o += `/**\n * ${CLASS_DOCS[name] || name + '.'}\n`;
  if (required.size) o += ` *\n * <p>Required by the API: ${[...required].filter((r) => fields.some((f) => f.json === r)).map((r) => `{@code ${r}}`).join(', ')}.\n`;
  o += ' */\n';
  o += '@JsonInclude(JsonInclude.Include.NON_NULL)\n@JsonIgnoreProperties(ignoreUnknown = true)\n';
  o += '@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,\n';
  o += '        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)\n';
  o += `public final class ${name} {\n`;
  for (const f of fields) o += `\n    @JsonProperty("${f.json}")\n    private ${f.type.java} ${f.name};\n`;

  o += `\n    /** Creates an empty instance. */\n    public ${name}() {\n    }\n`;
  if (isIdentifierPair) {
    o += `\n    /**\n     * Creates an identifier without a scheme.\n     *\n     * @param identifier the identifier value\n     * @return the new instance\n     */\n`;
    o += `    public static ${name} of(String identifier) {\n        return new ${name}().setIdentifier(identifier);\n    }\n`;
    o += `\n    /**\n     * Creates an identifier with a scheme.\n     *\n     * @param identifier the identifier value\n     * @param schemeId the scheme identifier\n     * @return the new instance\n     */\n`;
    o += `    public static ${name} of(String identifier, String schemeId) {\n        return new ${name}().setIdentifier(identifier).setSchemeId(schemeId);\n    }\n`;
  }
  if (isNote) {
    o += `\n    /**\n     * Creates a note without a subject code. Also used by Jackson to read the bare string form.\n     *\n     * @param note the note text\n     * @return the new instance\n     */\n`;
    o += `    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)\n    public static InvoiceNote of(String note) {\n        return new InvoiceNote().setNote(note);\n    }\n`;
    o += `\n    /**\n     * Creates a note with a UNTDID 4451 subject code.\n     *\n     * @param note the note text\n     * @param subjectCode the subject code, for example {@code "AAB"}\n     * @return the new instance\n     */\n`;
    o += `    public static InvoiceNote of(String note, String subjectCode) {\n        return new InvoiceNote().setNote(note).setSubjectCode(subjectCode);\n    }\n`;
  }

  for (const f of fields) {
    const C = cap(f.name);
    const doc = f.doc ? ` ${f.doc}` : '';
    const req = f.required ? ' Required.' : '';
    o += `\n    /**\n     * Returns the value of {@code ${f.json}}.${doc}${req}\n     *\n     * @return the value, or {@code null} when not set\n     */\n`;
    o += `    public ${f.type.java} get${C}() {\n        return ${f.name};\n    }\n`;
    o += `\n    /**\n     * Sets {@code ${f.json}}.${doc}${req}\n     *\n     * @param ${f.name} the new value, or {@code null} to omit it\n     * @return this instance\n     */\n`;
    o += `    public ${name} set${C}(${f.type.java} ${f.name}) {\n        this.${f.name} = ${f.name};\n        return this;\n    }\n`;
    if (f.type.list) {
      const item = f.type.list.java;
      const S = cap(singular(f.name));
      o += `\n    /**\n     * Appends one entry to {@code ${f.json}}.\n     *\n     * @param value the entry to append\n     * @return this instance\n     */\n`;
      o += `    public ${name} add${S}(${item} value) {\n        if (${f.name} == null) {\n            ${f.name} = new ArrayList<>();\n        }\n        ${f.name}.add(value);\n        return this;\n    }\n`;
      if (item === 'InvoiceNote') {
        o += `\n    /**\n     * Appends a plain text note to {@code ${f.json}}.\n     *\n     * @param note the note text\n     * @return this instance\n     */\n`;
        o += `    public ${name} add${S}(String note) {\n        return add${S}(InvoiceNote.of(note));\n    }\n`;
      }
    }
  }

  // equals / hashCode / toString
  const eq = fields.map((f) => (f.type.java === 'byte[]' ? `Arrays.equals(${f.name}, that.${f.name})` : `Objects.equals(${f.name}, that.${f.name})`));
  o += `\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) {\n            return true;\n        }\n        if (!(o instanceof ${name})) {\n            return false;\n        }\n`;
  if (fields.length) o += `        ${name} that = (${name}) o;\n        return ${eq.join('\n                && ')};\n    }\n`;
  else o += '        return true;\n    }\n';
  const hashParts = fields.map((f) => (f.type.java === 'byte[]' ? `Arrays.hashCode(${f.name})` : f.name));
  o += `\n    @Override\n    public int hashCode() {\n        return Objects.hash(${hashParts.join(', ')});\n    }\n`;
  const ts = fields.map((f, i) => {
    const v = f.type.java === 'byte[]' ? `(${f.name} == null ? "null" : ${f.name}.length + " bytes")` : f.name;
    return `"${i ? ', ' : ''}${f.name}=" + ${v}`;
  });
  o += `\n    @Override\n    public String toString() {\n        return "${name}{"${ts.length ? '\n                + ' + ts.join('\n                + ') : ''}\n                + "}";\n    }\n}\n`;
  write(`${name}.java`, o);
}

function write(file, content) {
  if (content.includes(String.fromCharCode(0x2014))) throw new Error(`Em dash in generated ${file}`);
  if (/\bB[TG]-\d/.test(content)) throw new Error(`Business term code in generated ${file}`);
  fs.writeFileSync(path.join(outDir, file), content);
  written.push(file);
}

for (const n of [...reachable].sort()) emitClass(n);

// PdfLanguage enum
write('PdfLanguage.java', HEADER + `package ${pkg};

/**
 * Language of the rendered invoice PDF template.
 */
public enum PdfLanguage {
    /** English. */
    EN,
    /** German. */
    DE,
    /** French. */
    FR
}
`);

// configured string enums
for (const cfg of Object.values(config.stringEnums || {})) {
  const vals = Object.entries(cfg.values);
  write(`${cfg.type}.java`, HEADER + `package ${pkg};

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * ${cfg.doc}
 */
public enum ${cfg.type} {
${vals.map(([k, v]) => `    /** ${cfg.valueDocs?.[k] ? cfg.valueDocs[k] + ' ' : ''}Wire value {@code "${v}"}. */\n    ${k}("${v}")`).join(',\n')};

    private final String value;

    ${cfg.type}(String value) {
        this.value = value;
    }

    /**
     * Returns the value sent on the wire.
     *
     * @return the wire value
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Parses a wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching constant
     * @throws IllegalArgumentException when the value is unknown
     */
    @JsonCreator
    public static ${cfg.type} fromValue(String value) {
        for (${cfg.type} v : values()) {
            if (v.value.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ${cfg.type}: " + value);
    }
}
`);
}

// code constant holders
write('VatCategoryCodes.java', HEADER + `package ${pkg};

/**
 * UNTDID 5305 VAT category codes used by EN 16931.
 */
public final class VatCategoryCodes {
    /** Standard rate. */
    public static final String STANDARD_RATE = "S";
    /** Zero rated goods. */
    public static final String ZERO_RATED = "Z";
    /** Exempt from tax. */
    public static final String EXEMPT = "E";
    /** VAT reverse charge. */
    public static final String REVERSE_CHARGE = "AE";
    /** VAT exempt for EEA intra-community supply of goods and services. */
    public static final String INTRA_COMMUNITY_SUPPLY = "K";
    /** Free export item, VAT not charged. */
    public static final String EXPORT_OUTSIDE_EU = "G";
    /** Services outside scope of tax. */
    public static final String OUTSIDE_SCOPE = "O";
    /** Canary Islands general indirect tax. */
    public static final String CANARY_ISLANDS_IGIC = "L";
    /** Tax for production, services and importation in Ceuta and Melilla. */
    public static final String CEUTA_MELILLA_IPSI = "M";

    private VatCategoryCodes() {
    }
}
`);
write('InvoiceTypeCodes.java', HEADER + `package ${pkg};

/**
 * UNTDID 1001 invoice type codes accepted by the API.
 */
public final class InvoiceTypeCodes {
    /** Partial invoice. */
    public static final String PARTIAL_INVOICE = "326";
    /** Commercial invoice. */
    public static final String INVOICE = "380";
    /** Credit note. */
    public static final String CREDIT_NOTE = "381";
    /** Debit note. */
    public static final String DEBIT_NOTE = "383";
    /** Corrected invoice. */
    public static final String CORRECTED_INVOICE = "384";
    /** Prepayment invoice. */
    public static final String PREPAYMENT_INVOICE = "386";
    /** Self-billed invoice. */
    public static final String SELF_BILLED_INVOICE = "389";
    /** Partial construction invoice. */
    public static final String PARTIAL_CONSTRUCTION_INVOICE = "875";
    /** Partial final construction invoice. */
    public static final String PARTIAL_FINAL_CONSTRUCTION_INVOICE = "876";
    /** Final construction invoice. */
    public static final String FINAL_CONSTRUCTION_INVOICE = "877";

    private InvoiceTypeCodes() {
    }
}
`);

write('package-info.java', HEADER + `/**
 * EN 16931 invoice model used by the create and extract endpoints, generated from
 * the InvoiceXML OpenAPI schema.
 */
package ${pkg};
`);

console.log(`Wrote ${written.length} files to ${config.outDir}`);
