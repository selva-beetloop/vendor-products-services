# vendor-products-services

Spring Boot backend for the Beetloop vendor portal's **Products** tab
(`/vendor/products-services` → *Add Product* wizard).

Covers all five product categories — Raw Materials, Processing Machinery,
Finished Goods, Packaging Materials, Packaging Machinery — and both required
save modes (step-based and overall), plus Submit-for-QC and the QC workflow.

---

## 1. Run it

Requires **JDK 17+** (built and tested on 21) and Maven.

```bash
cd vendor-products-services && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

| | |
|---|---|
| Base URL | `http://localhost:8086/vendor-products` |
| Swagger UI | http://localhost:8086/vendor-products/swagger-ui.html |
| OpenAPI JSON | http://localhost:8086/vendor-products/v3/api-docs |
| H2 console | http://localhost:8086/vendor-products/h2-console |

### Database

**H2, file-backed, is the default** so the service runs with no setup. The
schema is created by Hibernate (`ddl-auto: update`) into `./data/vendor-products.mv.db`.

For PostgreSQL:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

| Env var | Default |
|---|---|
| `SERVER_PORT` | `8086` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/vendor_products` |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` |
| `UPLOAD_DIR` | `./data/uploads` |
| `CORS_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` |

Deleting `./data/` resets the database.

### Tests

`./e2e-test.sh` drives the whole API for every category — create → Step 1 →
Step 2 → Add Variant (all five sub-steps) → sub-step save → overall save →
Submit for QC → QC approve, plus validation and listing checks.

```bash
./e2e-test.sh          # 117 assertions, expects the service on :8086
```

---

## 2. Endpoints

Everything is under `http://localhost:8086/vendor-products/api/vendor`.

### Products

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/products` | Create a draft when a category card is picked; returns the id used by every later step |
| `GET` | `/products` | Catalog listing — `search`, `category`, `status`, `page`, `size`, `sort` |
| `GET` | `/products/{id}` | Full nested product (identity + role + all variants) |
| `DELETE` | `/products/{id}` | Delete a product |

### Step-based save (mode A — each step independent)

| Method | Path | Purpose |
|---|---|---|
| `PUT` | `/products/{id}/identity` | Step 1 — Product / Machine Identity |
| `PUT` | `/products/{id}/role` | Step 2 — Your Role & Supply Information |
| `GET` | `/products/{id}/variants` | List variants |
| `POST` | `/products/{id}/variants` | Step 3 — add a variant (all five sub-steps in one body) |
| `PUT` | `/products/{id}/variants/{variantId}` | Update a variant |
| `PUT` | `/products/{id}/variants/{variantId}/{section}` | Save **one** variant sub-step |
| `DELETE` | `/products/{id}/variants/{variantId}` | Delete a variant |

`{section}` ∈ `details`, `technical-specifications`, `commercial-pricing`,
`compliance-documents`, `search-marketplace`.

Every step-save validates **only its own step** and preserves what earlier steps
stored. Send `"draft": true` to save partial progress without required-field
checks, so a vendor can leave the wizard and come back.

### Overall save (mode B) and QC

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/products/{id}/save` | Writes identity + role + variants in **one transaction**; omitted sections are left untouched. `submitForQc: true` submits in the same call |
| `POST` | `/products/{id}/submit` | Validates the assembled product and moves it to `SUBMITTED_FOR_QC` |
| `GET` | `/products/qc-review` | QC queue |
| `PUT` | `/products/{id}/qc-decision` | `APPROVE` / `REJECT` / `QUERY` / `PUBLISH` |

### Uploads and metadata

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/uploads` | Multipart upload; returns a stable file reference |
| `GET` | `/uploads/{id}` | Download a stored file |
| `GET` | `/catalog/categories` | All five categories with steps, type cards, role cards and fields |
| `GET` | `/catalog/categories/{category}` | One category's schema |
| `GET` | `/catalog/categories/{category}/identity-types` | Step 1 type cards |
| `GET` | `/catalog/categories/{category}/roles` | Step 2 role cards |
| `GET` | `/catalog/categories/{category}/fields` | Flattened field inventory |
| `GET` | `/catalog/shared-variant-sections` | Field definitions shared by every variant sub-step |

`{category}` is the kebab-case id (`raw-materials`, `processing-machinery`,
`finished-goods`, `packaging-materials`, `packaging-machinery`); the catalog
`groupId` (`materials`, `finished`, …) is accepted too.

---

## 3. Design notes

### Where the field definitions live

`src/main/resources/category-schemas.json` is the executable field mapping. Every
field name in it was taken from the frontend source of truth — the `*-data.ts`
form-state interfaces and the `validate()` blocks in each category's identity
page — so `required` mirrors exactly what the UI blocks *Save & Continue* on, and
error text matches the UI's own `"{label} is required"` wording.

It drives `CategoryFieldRegistry`, which powers both server-side validation and
`GET /catalog/categories`. Adding a field to the UI means adding one line there —
no entity or migration change.

### Why identity and role are JSON columns

Structural, cross-category data (status, listing summary, variants, spec groups,
price tiers, compliance documents) is modelled as real columns and relations.

The two sections whose field sets differ per category **and** per selected
type/role card — Step 1 identity and Step 2 role — are stored as JSON documents
validated against the registry. This is option (b) of §10.2 of the analysis
spec, chosen because the identity field set ranges from 24 fields (Raw Commodity)
to 40 (base Material Identification) with almost no overlap between categories,
and the role field set changes for each of up to 7 role cards per category. A
joined-table hierarchy would have meant ~30 tables and a migration per UI tweak.

Note on Raw Materials: the type card's form and the base "1.2 Product Identity"
block are **alternatives**, not layers — in `MaterialIdentificationPage.tsx` the
base block is the `else` branch, rendered only when the selected card has no
dedicated form of its own (today, `blend`). The registry models it that way.

### Status model

```
DRAFT → IDENTITY_SAVED → ROLE_SAVED → VARIANTS_SAVED → SUBMITTED_FOR_QC
                                                             ↓
                                                      PENDING_REVIEW
                                                       ↓     ↓     ↓
                                                APPROVED  REJECTED  QUERY
                                                    ↓
                                                PUBLISHED
```

Each status also carries the frontend's `StatusKind` (`draft` / `qc-pending` /
`query` / `published`) and badge label, so `GET /products` rows drop straight
into the catalog table. A product returned by QC (`REJECTED` / `QUERY`) becomes
editable again; `APPROVED` / `PUBLISHED` / in-review products reject edits with
`409`.

### Error shape

Every failure returns the same envelope. `fieldErrors` is keyed by the same field
names the wizard uses in its form state, so the frontend can do
`setErrors(err.fieldErrors)` with no translation:

```json
{
  "timestamp": "2026-08-13T13:45:54Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Product identity is incomplete",
  "path": "/vendor-products/api/vendor/products/{id}/identity",
  "errors": [{ "field": "botanicalName", "message": "Botanical Name is required" }],
  "fieldErrors": { "botanicalName": "Botanical Name is required" }
}
```

| Status | When |
|---|---|
| `400` | Validation failure, unknown field/type/role, unparseable body |
| `404` | Unknown product, variant or file id |
| `409` | Action not allowed from the current status |
| `413` | Upload over the size limit |

---

## 4. Frontend integration

The Next.js app in `../vendarfebeetloop` talks to this service through **new,
standalone** modules. No existing API client, endpoint or config file was
modified — the `/vendor/api/java/products/…` services on port 8085 and every
other client are untouched.

| File | Role |
|---|---|
| `api/vendorProductsServiceApi.ts` | Typed client; owns its own base URL |
| `hooks/useVendorProductListing.ts` | Wizard server-state (draft id, saves, loading, field errors) |
| `components/myProductsandServices_new/RawMaterial/rawMaterialRolePayload.ts` | Maps the role panel's displayed values into the Step 2 payload |

Point the frontend at a different host with:

```
NEXT_PUBLIC_VENDOR_PRODUCTS_API_BASE=http://localhost:8086/vendor-products
```

Wiring inside `MyCatalogPage.tsx` is additive: picking a category card opens a
draft, each *Save & Continue* persists its step, *Add Variant* posts the variant,
*Submit for QC* submits, and `GET /products` rows are merged into the catalog
listing ahead of the existing demo rows. Every call is best-effort — if the
backend is down the wizard keeps working exactly as before on local state.

See `../INTEGRATION-VERIFICATION.md` for the end-to-end evidence.
