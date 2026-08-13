# beetloop-catalog-service

Spring Boot 3.3 · Java 21 · MongoDB 7 implementation of the plan in
[`../03-SPRINGBOOT-PLAN.md`](../03-SPRINGBOOT-PLAN.md), with the endpoints from
[`../05-API-REFERENCE.md`](../05-API-REFERENCE.md).

The point of this service is that **both save models exist side by side** for products *and*
services:

```
STEP-WISE   PUT /vendor/products/{id}/steps/{stepKey}                       merge one step
            PUT /vendor/products/{id}/variants/{vid}/sections/{sectionKey}  merge one variant stage
            PUT /vendor/services/{id}/steps/{stepKey}
            PUT /vendor/services/{id}/configurations/{cid}/sections/{sectionKey}

OVERALL     POST /vendor/products/save-all        create-or-update the whole wizard, one transaction
            PUT  /vendor/products/{id}/save-all
            POST /vendor/services/save-all
            PUT  /vendor/services/{id}/save-all
```

Neither path ever changes `qcStatus`. Publishing needs `POST …/submit-qc` and a QC approval, and
there is deliberately no `submit` flag on `save-all`.

---

## Run it

```bash
docker compose up -d          # single-node replica set on :27017
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Transactions are required by the overall save, `submit-qc` and every QC transition, so the target
must be a **replica set** — a standalone `mongod` will not do. Without Docker:

```bash
mongod --replSet rs0 --dbpath /tmp/beetloop-mongo
mongosh --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"localhost:27017"}]})'
```

On first start `DataSeeder` loads `src/main/resources/seed/`: 12 categories, 190 vocabularies,
12 commercial-master rows and **all ten form templates**. Re-running is a no-op.

- API: `http://localhost:8080/api/v1`
- Swagger: `http://localhost:8080/api/v1/swagger`
- Health: `http://localhost:8080/api/v1/actuator/health`

### Minting a local token

The `local` profile verifies HS256 tokens so the API runs without an IdP. The vendor id comes from
the `vendor_id` claim and nowhere else.

```bash
python3 - <<'PY'
import base64, hmac, hashlib, json, time
S = "beetloop-local-development-secret-key-change-me-32b+"
b = lambda d: base64.urlsafe_b64encode(d).rstrip(b"=").decode()
def mint(sub, roles):
    now = int(time.time())
    h = b(json.dumps({"alg":"HS256","typ":"JWT"}).encode())
    p = b(json.dumps({"sub":sub,"vendor_id":"vnd_01HMYY3B8Q","roles":roles,
                      "iat":now,"exp":now+86400}).encode())
    return f"{h}.{p}." + b(hmac.new(S.encode(), f"{h}.{p}".encode(), hashlib.sha256).digest())
print("VENDOR:", mint("usr_01HMYY5C", ["VENDOR"]))
print("QC    :", mint("usr_qc_04", ["QC_REVIEWER"]))
PY
```

In `dev`/`staging`/`prod` drop the profile and set `JWT_JWK_SET_URI` instead.

---

## What is where

```
com.beetloop.catalog
├── template/          the form-template engine: schema model, ValidationEngine, DerivationService
├── product/           listings, step-wise save, variants + section save, overall save, submit
├── servicelisting/    the mirror, with configurations instead of variants
├── qc/                queue, claim, approve, reject, status history (ReviewableListingPort)
├── document/          uploads, the shared accreditation library, link rows, expiry derivation
├── customvalue/       the "+ Add" chip values that currently vanish on reload
├── masters/           categories, vocabularies (incl. cascades), master-catalogue search
├── catalog/           the two listing tabs, KPI tiles, facet counts, bulk actions
├── facility/          the read-only facility snapshot projected into step 2
├── audit/             append-only events, including the Packaging Materials attestation
├── shared/            error catalogue, tenant context, idempotency, value types
└── seed/              DataSeeder + src/main/resources/seed/**
```

### The template engine is the load-bearing part

Nine Raw Material type cards, seven supply roles, an open-ended Packaging Machinery role set behind
`More Roles`, variant builders with 5 / 5 / 7 / 6 / 5 stages, service configurations with 3 / 5 / 5
/ 5 / 11 sub-steps, and an Agro Cluster field that switches the *next* sub-step's schema — all of it
is **data in `form_templates`**, not Java.

Adding a category should be one JSON file under `seed/templates/` and nothing else. There is no
category-specific controller, service or DTO anywhere in the codebase.

---

## Verified behaviour

Exercised against a live replica set during development:

| Behaviour | Result |
|---|---|
| Switching Raw Material cards | `blend` written, `botanicalExtract` preserved — a vendor can switch back |
| Client writes `facilitySnapshot` | `rejectedFields[{reason: READ_ONLY_LINKED}]`, save still succeeds |
| Client sets certificate `status` | rejected as `DERIVED_FIELD`; server derives `VALID` / `EXPIRED` |
| Free-text dates `12 May 2025`, `01/09/2020` | normalised to ISO-8601, pattern echoed in `dateInterpretation` |
| Certificate expired 2023-08-31 | `status: EXPIRED`, `daysExpired: 1075`, and **blocks submit** |
| Submit with empty wizard | `422`, `stepErrors {identity: 11, variants: 1}`, plus the exact banner the machinery wizards already show |
| Paid `promotionTier: PREMIUM` on a save | `rejectedFields[{reason: BILLING_GATED_FIELD}]` |
| Overlapping volume tiers | `422 TIERS_NOT_CONTIGUOUS` — *"Tier 2 starts at 5 but tier 1 ends at 9"* |
| Overall save omitting steps/variants | `clearedSteps`, `deletedVariants`, and `STEPS_CLEARED` / `VARIANTS_DELETED` warnings |
| `save-all` with stale `If-Match` | `409 BL-PS-409-STALE-VERSION` with `currentVersion` |
| `save-all` without `If-Match` | `428 BL-PS-428-IF-MATCH-REQUIRED` |
| Submit → claim → approve | `PENDING_REVIEW` → `IN_REVIEW` → `APPROVED` / `PUBLISHED` |
| QC reject without field feedback | `422 BL-PS-422-FEEDBACK-REQUIRED` |
| Lab Testing spec groups | per-group `completed/total` and `overallCompletion` derived; `analysisSummary` computed |
| Service submit with no accreditation linked | `422 REQUIRED_DOCUMENT_MISSING` |

---

## Deliberate deviations from the plan

| Plan | Here | Why |
|---|---|---|
| Gradle, 8 modules | Maven, one module, same package layout | The module boundaries are the package boundaries; a single module builds and runs with no extra ceremony. Split later without moving a class. |
| Mongock migrations | idempotent `DataSeeder` + `auto-index-creation` | One `docker compose up` and the service is populated. Swapping in Mongock changes only `DataSeeder`. |
| Redis cache | Caffeine | Same `spring-cache` abstraction; `spring.cache.type=redis` is a property change. |
| `ValidationRule` as a sealed interface | `RuleType` enum + `params` map | Rules are persisted inside template documents; a sealed hierarchy would couple the database to Java class names. The evaluator still switches exhaustively. |
| S3 storage | `LocalStorageGateway` behind `StorageGateway` | Runs without cloud credentials. Implement the port for S3 presigned PUT/GET without touching callers. |

Two fixes worth knowing about, both found by running the thing:

- **Service step keys.** URLs are kebab-case (`/steps/select-service`) and `data{}` keys are
  camelCase (`selectService`). The seeded templates emitted `dataKey == key`, so
  `selectionId` and `requestCode` were never minted. `FormTemplate.StepSchema` now carries both.
- **`BigDecimal` in MongoDB.** Spring Data stores it as a **String** by default, which silently
  broke every derived figure that adds prices up. `MongoDecimalConverters` maps it to `Decimal128`.

---

## Not implemented

Stubs or absent, and called out rather than quietly missing:

- **Bulk upload** (`variants/bulk-upload`) — the machinery-only XLSX import job.
- **Export** (`catalog/export`) — returns no job runner.
- **Promotion / billing** (`/vendor/products/{id}/promotion`) — the field is correctly *refused* on
  save with `BILLING_GATED_FIELD`, but the order endpoint and billing port are not built.
- **Search intents and quality score** — `buyerSearchIntents`, `indexedCategories` and
  `aiRerankBoost.qualityScore` are treated as derived and rejected on write, but no analytics
  read model produces them yet.
- **AV scanning** — uploads are marked `CLEAN` immediately; the `scanStatus` gate and its
  `409 DOCUMENT_NOT_READY` are wired and enforced.
- **Custom-value reference checking on delete** — the `409 CUSTOM_VALUE_IN_USE` path exists but the
  controller passes an empty reference list, so a delete currently always succeeds.
- **Tests.** There are none. The plan's per-category contract suite — run the real validators
  against the real template documents — is the first thing to add.

Seed templates carry every **required** field from
[`../09-VALIDATION-AND-ERRORS.md`](../09-VALIDATION-AND-ERRORS.md) §4.2 plus a representative set of
optional ones; they are not yet the complete field inventory from
[`../02-DATA-OBJECTS.md`](../02-DATA-OBJECTS.md). Filling them in is a JSON edit, not a code change —
which was the point.
