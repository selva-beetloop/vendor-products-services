# Vendor Products & Services — API Documentation

Base URL: `http://localhost:8086/vendor-products/api/vendor`
Interactive: `http://localhost:8086/vendor-products/swagger-ui.html`

## Conventions

**Headers**

| Header | Required | Notes |
|---|---|---|
| `Content-Type: application/json` | on writes | multipart for `/uploads` |
| `X-VENDOR-ID` | optional | scopes `GET /products` to one vendor |
| `X-USER-ID` | optional | recorded as `createdBy` / `uploadedBy` |

**Status codes**

| Code | Meaning |
|---|---|
| `200` | OK |
| `201` | Created (`POST /products`, `POST /variants`, `POST /uploads`) |
| `204` | No content (deletes) |
| `400` | Validation failure — see `fieldErrors` |
| `404` | Unknown id |
| `409` | Not allowed from the current status |
| `413` | Upload too large |

**Error body** — identical for every failure:

```json
{
  "timestamp": "2026-08-13T13:45:54.994Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Product identity is incomplete",
  "path": "/vendor-products/api/vendor/products/{id}/identity",
  "errors": [
    { "field": "botanicalName", "message": "Botanical Name is required", "rejectedValue": null }
  ],
  "fieldErrors": { "botanicalName": "Botanical Name is required" }
}
```

`fieldErrors` uses the same field names as the wizard's form state.

**Category ids**: `raw-materials`, `processing-machinery`, `finished-goods`,
`packaging-materials`, `packaging-machinery`. The catalog `groupId`
(`materials`, `finished`, `packaging-materials`, `packaging-machinery`,
`processing-machinery`) is also accepted wherever a category is taken.

---

## 1. `POST /products` — create a draft

Called when the vendor picks a category card on *List a Product*.

**Request**

```json
{
  "category": "raw-materials",
  "identityType": "botanical-extract",
  "sourceMasterId": "curcumin-95",
  "name": "Curcumin 95% Extract"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `category` | enum | yes | one of the five ids |
| `identityType` | string | no | type card, when the category has a selector |
| `sourceMasterId` | string | no | master-catalog record the vendor started from |
| `name` | string | no | working name until Step 1 is saved |

**Response `201`** — the full product document (see §3).

```json
{ "id": "09982ad3-af9f-4ff1-b3d4-673e713f78ac", "category": "raw-materials",
  "status": "DRAFT", "statusKind": "draft", "statusLabel": "Draft", "…": "…" }
```

**Errors**: `400` when `category` is missing or unknown.

---

## 2. `GET /products` — catalog listing

**Query parameters**

| Param | Default | Notes |
|---|---|---|
| `search` | — | matches name, SKU or category |
| `category` | all | category id or catalog groupId |
| `status` | all | `DRAFT`, `SUBMITTED_FOR_QC`, … or a StatusKind like `qc-pending` |
| `page` | `0` | zero-based |
| `size` | `10` | |
| `sort` | `updatedAt,desc` | `property,asc\|desc` |

**Response `200`** — rows mirror the frontend's `CatalogProduct`:

```json
{
  "content": [{
    "id": "09982ad3-af9f-4ff1-b3d4-673e713f78ac",
    "name": "Curcumin 95% Extract",
    "sku": "CUR-95-001",
    "hasTypeBadge": true,
    "verified": false,
    "type": "Material",
    "groupId": "materials",
    "category": "Standardized Extract",
    "functionalRole": ["manufacturer"],
    "applications": [],
    "originFlag": "🇮🇳",
    "originCountry": "India",
    "documents": ["FSSAI License"],
    "documentsExtra": 0,
    "sample": "available",
    "inventoryQty": "—",
    "inventoryBatches": "—",
    "status": "qc-pending",
    "statusLabel": "QC Pending",
    "statusCode": "SUBMITTED_FOR_QC",
    "actionIcon": "eye",
    "thumbEmoji": "🧪",
    "variantCount": 1,
    "createdAt": "2026-08-13T16:31:02Z",
    "updatedAt": "2026-08-13T16:33:05Z"
  }],
  "page": 0, "size": 10, "totalElements": 7, "totalPages": 1,
  "first": true, "last": true
}
```

---

## 3. `GET /products/{id}` — full detail

Returns everything needed to rehydrate the wizard.

```json
{
  "id": "09982ad3-…",
  "category": "raw-materials",
  "groupId": "materials",
  "status": "SUBMITTED_FOR_QC",
  "statusKind": "qc-pending",
  "statusLabel": "QC Pending",
  "name": "Curcumin 95% Extract",
  "sku": "CUR-95-001",
  "listingCategory": "Standardized Extract",
  "originCountry": "India",
  "thumbEmoji": "🧪",
  "verified": false,
  "sourceMasterId": "curcumin-95",

  "productIdentity": {
    "identityType": "botanical-extract",
    "data": { "extractName": "Curcumin 95% Extract", "botanicalName": "Curcuma longa", "…": "…" }
  },
  "yourRole": {
    "roleId": "manufacturer",
    "data": { "selectFacilityCompany": "…", "legalEntityName": "Verdant Biotics Pvt Ltd", "…": "…" }
  },
  "variants": [ /* §6 */ ],

  "qc": { "reviewer": null, "remarks": null,
          "submittedAt": "2026-08-13T16:33:05Z", "reviewedAt": null },
  "completion": { "identity": true, "role": true, "variants": true, "submitted": true },
  "createdAt": "…", "updatedAt": "…"
}
```

**Errors**: `404` if the id does not exist.

---

## 4. `PUT /products/{id}/identity` — Step 1

Validates **only** Step 1; everything else is preserved.

**Request**

```json
{
  "identityType": "botanical-extract",
  "data": {
    "extractName": "Curcumin 95% Extract",
    "botanicalName": "Curcuma longa",
    "plantPartUsed": "Rhizome",
    "extractType": "Standardized Extract",
    "extractionMethod": "Solvent Extraction",
    "markerCompound": "Curcumin",
    "markerAssay": "≥ 95",
    "standardizationType": "By HPLC",
    "countryOfOrigin": "India",
    "purityCurcuminoids": "≥ 95"
  },
  "draft": false
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `identityType` | string | when the category has a type selector | falls back to the value from create |
| `data` | object | yes | flat map of field name → value |
| `draft` | boolean | no (`false`) | `true` stores partial progress and skips required-field checks |

`data` keys are validated against the category's schema: unknown keys are
rejected, and fields with a fixed option list must use one of those values.

**Response `200`** — the full product, `status` advanced to `IDENTITY_SAVED`.

**Validation error `400`**

```json
{
  "status": 400, "error": "Validation Failed",
  "message": "Product identity is incomplete",
  "fieldErrors": {
    "botanicalName": "Botanical Name is required",
    "plantPartUsed": "Plant Part Used is required"
  }
}
```

**Errors**: `400` validation / unknown type; `404` unknown id; `409` product is
approved, published or in review.

---

## 5. `PUT /products/{id}/role` — Step 2

**Request**

```json
{
  "roleId": "manufacturer",
  "data": {
    "selectFacilityCompany": "Verdant Biotics Manufacturing Facility - Goa, India",
    "legalEntityName": "Verdant Biotics Pvt Ltd",
    "gstin": "30AABCV1234B1Z5",
    "iecCode": "0412345678",
    "businessType": "Manufacturer"
  },
  "draft": false
}
```

| Field | Type | Required |
|---|---|---|
| `roleId` | string | yes — must be one of the category's role cards |
| `data` | object | yes |
| `draft` | boolean | no |

**Response `200`** — full product, `status` advanced to `ROLE_SAVED`.

**Errors**: `400` (`{"roleId": "Select your role"}` or per-field messages);
`404`; `409`.

---

## 6. Variants — Step 3

### `POST /products/{id}/variants` — add a variant

All five sub-steps in one body; every section is optional.

```json
{
  "variantDetails": {
    "name": "Food Grade 1kg Pouch",
    "variantType": "Pack Size",
    "grade": "Food Grade",
    "assayPurity": "95%",
    "packSize": "1 kg",
    "packagingType": "Aluminum Pouch",
    "particleSize": "80 Mesh",
    "skuCode": "CUR-FG-1KG",
    "batchPrefix": "CUR95-FG",
    "status": "Active",
    "images": [],
    "extra": {}
  },
  "technicalSpecifications": {
    "data": [{
      "title": "Assay & Purity Specification",
      "tag": "Primary",
      "collapsed": false,
      "data": [{
        "parameterName": "Assay / Purity",
        "specification": "≥ 95.0",
        "unit": "%",
        "testMethodOrStandard": "HPLC / Titration",
        "requirementSource": "Buyer Specification",
        "attachment": null,
        "priority": "High",
        "remarks": ""
      }]
    }]
  },
  "commercialPricing": {
    "pricingQuantity": {
      "basePricing": { "pricePerUnit": 4500.00, "unit": "kg", "moq": "25", "leadTime": "2 Weeks" },
      "volumePricing": [
        { "quantityRange": "25-100 kg", "tierName": "Base Price", "pricePerUnit": "4500",
          "discountVsBase": "0%", "leadTime": "14" }
      ],
      "commercialCharges": {
        "freightCharges": "Extra (At Actuals)", "insuranceCharges": "Extra (At Actuals)",
        "handlingCharges": "", "otherCharges": ""
      }
    },
    "commercialTradeTerms": {
      "currency": "INR (₹)", "paymentTerms": "Net 30 Days", "incoterms": "EXW (Ex Works)",
      "priceValidityDays": "30", "gstTaxes": "Excl. of GST",
      "exportAvailable": true, "minOrderValue": "", "partialShipmentAllowed": false,
      "returnPolicy": "", "warrantyPeriod": ""
    },
    "packagingAndSamples": {
      "packaging": [{ "packagingType": "Aluminum Pouch", "size": "1 kg", "customPackaging": "No" }],
      "sampleInformation": {
        "sampleAvailable": true, "freePaidSample": "Free", "sampleCost": "",
        "sampleTurnaroundDays": "5", "maxSampleQty": "100 g", "sampleShippingBorneBy": "Buyer"
      }
    },
    "includedDocumentsAndServices": {
      "documentsIncluded": ["CoA", "TDS", "SDS"],
      "servicesIncluded": ["Application Support"]
    }
  },
  "complianceCertifications": {
    "data": [{
      "category": "Standard Certification", "name": "FSSAI License",
      "reference": "10012345000123", "authority": "FSSAI", "applicableTo": "All Grades",
      "date": "12 May 2025", "expiryDate": "11 May 2027", "status": "Active",
      "fileName": "fssai.pdf", "fileId": null, "fileUrl": null
    }]
  },
  "searchMarketplace": {
    "searchTagsAndKeywords": ["curcumin", "turmeric extract"],
    "seoKeywords": "curcumin 95 botanical extract",
    "synonymsOrAlternativeNames": ["Curcuma longa extract"],
    "negativeKeywords": ["curry powder"]
  },
  "draft": false
}
```

Validation mirrors the UI: `variantDetails.name` is required; a spec parameter
row is either **entirely blank** (ignored, matching the UI's own
`isSpecRowComplete` check) or must have parameter, specification, unit, method
and source; a compliance document needs a `name`.

**Response `201`** — the saved variant, echoed in the same nesting plus an `id`
and a flat `summary` for the variants table:

```json
{
  "id": "bbb425e5-…", "position": 0,
  "variantDetails": { "…": "…" },
  "technicalSpecifications": { "…": "…" },
  "commercialPricing": { "…": "…" },
  "complianceCertifications": { "…": "…" },
  "searchMarketplace": { "…": "…" },
  "summary": { "name": "Food Grade 1kg Pouch", "skuCode": "CUR-FG-1KG",
               "specificationCount": 1, "parameterCount": 1, "documentCount": 1 },
  "createdAt": "…", "updatedAt": "…"
}
```

### `PUT /products/{id}/variants/{variantId}`

Same body; replaces every section present in the payload.

### `PUT /products/{id}/variants/{variantId}/{section}` — one sub-step

`{section}` ∈ `details` · `technical-specifications` · `commercial-pricing` ·
`compliance-documents` · `search-marketplace`.

Send only that section:

```json
{ "searchMarketplace": { "seoKeywords": "updated keywords" } }
```

Lets the vendor leave the Add Variant wizard mid-way without losing data.
**Response `200`** — the updated variant.

### `GET /products/{id}/variants` · `DELETE /products/{id}/variants/{variantId}`

List (`200`, array) and delete (`204`).

---

## 7. `POST /products/{id}/save` — overall save

Writes every provided section in one transaction; omitted sections are left
untouched, so a partially inconsistent product cannot be produced.

```json
{
  "productIdentity": { "identityType": "botanical-extract", "data": { "…": "…" } },
  "yourRole": { "roleId": "manufacturer", "data": { "…": "…" } },
  "variants": [ { "variantDetails": { "…": "…" } } ],
  "draft": false,
  "submitForQc": false
}
```

| Field | Notes |
|---|---|
| `productIdentity` / `yourRole` | same shapes as §4 / §5 |
| `variants` | when present, **replaces** the whole variant list |
| `draft` | `true` skips required-field checks across all sections |
| `submitForQc` | `true` validates and submits in the same call |

With `draft: false` the whole product is validated (all three steps present and
individually valid) before anything is written.

**Response `200`** — full product. **Errors**: `400`, `404`, `409`.

---

## 8. `POST /products/{id}/submit` — Submit for QC

No body. Validates the fully-assembled product, then sets
`SUBMITTED_FOR_QC`, stamps `submittedAt` and generates a vendor SKU if absent.

**Response `200`**

```json
{ "id": "09982ad3-…", "status": "SUBMITTED_FOR_QC", "statusKind": "qc-pending",
  "statusLabel": "QC Pending", "sku": "CUR-95-001",
  "qc": { "submittedAt": "2026-08-13T16:33:05Z" }, "…": "…" }
```

**Errors**

| Code | Cause |
|---|---|
| `400` | A step is incomplete — e.g. `{"variants": "Add at least one variant before submitting"}` |
| `409` | Already submitted, approved or published |

---

## 9. QC workflow

### `GET /products/qc-review`

Products in `SUBMITTED_FOR_QC` or `PENDING_REVIEW`. Same page envelope and row
shape as `GET /products`. Params: `search`, `page`, `size`, `sort`.

### `PUT /products/{id}/qc-decision`

```json
{ "decision": "APPROVE", "reviewer": "qc.reviewer", "remarks": "Looks good" }
```

| `decision` | Result |
|---|---|
| `APPROVE` | `APPROVED`, `verified: true` |
| `PUBLISH` | `PUBLISHED`, `verified: true` |
| `REJECT` | `REJECTED` — editable again; **remarks required** |
| `QUERY` | `QUERY` — editable again; **remarks required** |

**Errors**: `400` (unknown decision, or missing remarks on REJECT/QUERY);
`409` when the product is not awaiting review.

---

## 10. `POST /uploads` — file upload

`multipart/form-data`.

| Part / param | Notes |
|---|---|
| `file` | the file (required) |
| `module` | query param; default `product-document`. Containing `image` switches validation to image types |
| `referenceId` | query param, optional |

Accepted: documents `pdf, doc, docx, xls, xlsx, csv, txt`; images
`png, jpg, jpeg, webp, gif, svg`. Max 10 MB.

**Response `201`**

```json
{
  "id": "3f2a…", "fileName": "fssai.pdf", "contentType": "application/pdf",
  "sizeBytes": 254122, "url": "/api/vendor/uploads/3f2a…",
  "module": "compliance-document", "uploadedAt": "2026-08-13T16:40:00Z"
}
```

Put the returned `id`/`url` on a compliance document (`fileId`, `fileUrl`) or a
spec parameter (`attachment`).

**Errors**: `400` (no file, unsupported type, oversize); `413`.

`GET /uploads/{id}` streams the file back.

---

## 11. Catalog metadata

| Endpoint | Returns |
|---|---|
| `GET /catalog/categories` | All five categories: label, groupId, action-button label, steps, identity type cards, role cards, variant sub-steps and every field definition |
| `GET /catalog/categories/{category}` | One category's schema |
| `GET /catalog/categories/{category}/identity-types` | `["raw-commodity","processed-commodity",…]` — empty for categories without a type selector |
| `GET /catalog/categories/{category}/roles` | `["manufacturer","processor",…]` |
| `GET /catalog/categories/{category}/fields` | Flattened: `identityBaseFields`, `identityTypes`, `roles`, `variantDetailFields`, `variantExtraFields` |
| `GET /catalog/shared-variant-sections` | Field definitions shared by every variant sub-step |

A field definition looks like:

```json
{
  "name": "plantPartUsed",
  "label": "Plant Part Used",
  "type": "select",
  "required": true,
  "options": ["Rhizome", "Root", "Leaf", "Fruit", "Bark", "Seed", "Flower", "Whole Plant"],
  "multiple": false,
  "dependsOn": null,
  "errorMessage": null
}
```

`type` ∈ `text`, `textarea`, `select`, `multiselect`, `number`, `date`,
`checkbox`, `toggle`, `tags`, `image`, `file`, `repeatable`, `object`.

---

## 12. Worked example — full Raw Materials run

```bash
B=http://localhost:8086/vendor-products/api/vendor

# 1. draft
PID=$(curl -s -X POST $B/products -H 'Content-Type: application/json' \
  -d '{"category":"raw-materials","identityType":"botanical-extract"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

# 2. Step 1
curl -s -X PUT $B/products/$PID/identity -H 'Content-Type: application/json' -d '{
  "identityType":"botanical-extract",
  "data":{"extractName":"Curcumin 95% Extract","botanicalName":"Curcuma longa",
          "plantPartUsed":"Rhizome","extractType":"Standardized Extract",
          "extractionMethod":"Solvent Extraction","markerCompound":"Curcumin",
          "markerAssay":"≥ 95","standardizationType":"By HPLC",
          "countryOfOrigin":"India","purityCurcuminoids":"≥ 95"}}'

# 3. Step 2
curl -s -X PUT $B/products/$PID/role -H 'Content-Type: application/json' -d '{
  "roleId":"manufacturer",
  "data":{"selectFacilityCompany":"Verdant Biotics Manufacturing Facility - Goa, India",
          "legalEntityName":"Verdant Biotics Pvt Ltd"}}'

# 4. Step 3
curl -s -X POST $B/products/$PID/variants -H 'Content-Type: application/json' \
  -d '{"variantDetails":{"name":"Food Grade 1kg Pouch","packSize":"1 kg"}}'

# 5. Submit for QC
curl -s -X POST $B/products/$PID/submit

# 6. QC approve
curl -s -X PUT $B/products/$PID/qc-decision -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE","reviewer":"qc.reviewer","remarks":"Looks good"}'
```

`./e2e-test.sh` runs this for all five categories plus validation and listing
checks — 117 assertions.
