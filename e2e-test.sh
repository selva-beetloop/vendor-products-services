#!/usr/bin/env bash
# End-to-end smoke test of the vendor-products-services API.
# Exercises, for every category: create draft → Step 1 → Step 2 → Add Variant
# (all five sub-steps) → overall save → Submit for QC → QC decision.
#
# Usage: ./e2e-test.sh [base-url]
set -uo pipefail

BASE="${1:-http://localhost:8086/vendor-products}/api/vendor"
PASS=0
FAIL=0

# Reads JSON on stdin and prints the value of a Python expression over `d`.
# The expression is passed via argv (not interpolated into the -c string), so
# quotes inside it survive.
jqp() { python3 -c 'import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))' "d$1" 2>/dev/null; }

check() { # check <label> <actual> <expected>
  if [[ "$2" == "$3" ]]; then
    echo "  ✓ $1"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $1 — expected '$3', got '$2'"
    FAIL=$((FAIL + 1))
  fi
}

status_of() { # status_of <method> <path> [body]
  local method=$1 path=$2 body=${3:-}
  if [[ -n "$body" ]]; then
    curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE$path" \
      -H 'Content-Type: application/json' -H 'X-VENDOR-ID: vend-001' -d "$body"
  else
    curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE$path" -H 'X-VENDOR-ID: vend-001'
  fi
}

post() { curl -s -X POST "$BASE$1" -H 'Content-Type: application/json' -H 'X-VENDOR-ID: vend-001' -d "$2"; }
put()  { curl -s -X PUT  "$BASE$1" -H 'Content-Type: application/json' -H 'X-VENDOR-ID: vend-001' -d "$2"; }
get()  { curl -s "$BASE$1" -H 'X-VENDOR-ID: vend-001'; }

# ---------------------------------------------------------------- variant body
variant_payload() {
cat <<'JSON'
{
  "variantDetails": {
    "name": "Food Grade 1kg Pouch", "variantType": "Pack Size", "grade": "Food Grade",
    "assayPurity": "95%", "packSize": "1 kg", "packagingType": "Aluminum Pouch",
    "particleSize": "80 Mesh", "skuCode": "CUR-FG-1KG", "batchPrefix": "CUR95-FG",
    "status": "Active", "images": []
  },
  "technicalSpecifications": {
    "data": [{
      "title": "Assay & Purity Specification", "tag": "Primary", "collapsed": false,
      "data": [{
        "parameterName": "Assay / Purity", "specification": "≥ 95.0", "unit": "%",
        "testMethodOrStandard": "HPLC / Titration", "requirementSource": "Buyer Specification"
      }]
    }]
  },
  "commercialPricing": {
    "pricingQuantity": {
      "basePricing": { "pricePerUnit": 4500.00, "unit": "kg", "moq": "25", "leadTime": "2 Weeks" },
      "volumePricing": [
        { "quantityRange": "25-100 kg", "tierName": "Base Price", "pricePerUnit": "4500",
          "discountVsBase": "0%", "leadTime": "14" },
        { "quantityRange": "100+ kg", "tierName": "Bulk", "pricePerUnit": "4200",
          "discountVsBase": "6.7%", "leadTime": "21" }
      ],
      "commercialCharges": { "freightCharges": "Extra (At Actuals)", "insuranceCharges": "Extra (At Actuals)" }
    },
    "commercialTradeTerms": {
      "currency": "INR (₹)", "paymentTerms": "Net 30 Days", "incoterms": "EXW (Ex Works)",
      "priceValidityDays": "30", "gstTaxes": "Excl. of GST", "exportAvailable": true,
      "partialShipmentAllowed": false
    },
    "packagingAndSamples": {
      "packaging": [{ "packagingType": "Aluminum Pouch", "size": "1 kg", "customPackaging": "No" }],
      "sampleInformation": {
        "sampleAvailable": true, "freePaidSample": "Free", "sampleTurnaroundDays": "5",
        "maxSampleQty": "100 g", "sampleShippingBorneBy": "Buyer"
      }
    },
    "includedDocumentsAndServices": {
      "documentsIncluded": ["CoA", "TDS", "SDS"],
      "servicesIncluded": ["Application Support"]
    }
  },
  "complianceCertifications": {
    "data": [{
      "category": "Standard Certification", "name": "FSSAI License", "reference": "10012345000123",
      "authority": "FSSAI", "applicableTo": "All Grades", "date": "12 May 2025",
      "expiryDate": "11 May 2027", "status": "Active", "fileName": "fssai.pdf"
    }]
  },
  "searchMarketplace": {
    "searchTagsAndKeywords": ["curcumin", "turmeric extract"],
    "seoKeywords": "curcumin 95 botanical extract",
    "synonymsOrAlternativeNames": ["Curcuma longa extract"],
    "negativeKeywords": ["curry powder"]
  }
}
JSON
}

# ---------------------------------------------------------------- per-category
run_category() { # run_category <label> <category> <identityType|-> <identityJson> <roleId> <roleJson>
  local label=$1 category=$2 itype=$3 idata=$4 role=$5 rdata=$6
  echo
  echo "=== $label ==="

  local create_body
  if [[ "$itype" == "-" ]]; then
    create_body="{\"category\":\"$category\"}"
  else
    create_body="{\"category\":\"$category\",\"identityType\":\"$itype\"}"
  fi

  local pid
  pid=$(post "/products" "$create_body" | jqp "['id']")
  if [[ -z "$pid" ]]; then echo "  ✗ create failed"; FAIL=$((FAIL+1)); return; fi
  echo "  productId=$pid"

  # Step 1
  local ibody
  if [[ "$itype" == "-" ]]; then
    ibody="{\"data\":$idata}"
  else
    ibody="{\"identityType\":\"$itype\",\"data\":$idata}"
  fi
  check "Step 1 identity saved" "$(status_of PUT "/products/$pid/identity" "$ibody")" "200"

  # Step 2
  local role_body="{\"roleId\":\"$role\",\"data\":$rdata}"
  check "Step 2 role saved" "$(status_of PUT "/products/$pid/role" "$role_body")" "200"
  check "Step 2 role persisted" "$(get "/products/$pid" | jqp "['yourRole'].get('roleId')")" "$role"

  # Step 3 — add variant with all five sub-steps
  local vid
  vid=$(post "/products/$pid/variants" "$(variant_payload)" | jqp "['id']")
  if [[ -n "$vid" ]]; then
    echo "  ✓ Step 3 variant added ($vid)"; PASS=$((PASS+1))
  else
    echo "  ✗ Step 3 variant add failed"; FAIL=$((FAIL+1))
  fi

  # Individual sub-step save
  check "variant sub-step save (search-marketplace)" \
    "$(status_of PUT "/products/$pid/variants/$vid/search-marketplace" \
       '{"searchMarketplace":{"seoKeywords":"updated keywords"}}')" "200"

  # Round-trip: what we saved is what we read back
  local detail tiers docs specs
  detail=$(get "/products/$pid")
  check "round-trip variant count"   "$(echo "$detail" | jqp "['variants'].__len__()")" "1"
  check "round-trip price tiers"     "$(echo "$detail" | jqp "['variants'][0]['commercialPricing']['pricingQuantity']['volumePricing'].__len__()")" "2"
  check "round-trip spec parameters" "$(echo "$detail" | jqp "['variants'][0]['technicalSpecifications']['data'][0]['data'].__len__()")" "1"
  check "round-trip compliance docs" "$(echo "$detail" | jqp "['variants'][0]['complianceCertifications']['data'].__len__()")" "1"
  check "sub-step persisted"         "$(echo "$detail" | jqp "['variants'][0]['searchMarketplace']['seoKeywords']")" "updated keywords"

  # Overall save (idempotent re-send of the same sections)
  check "overall save" "$(status_of POST "/products/$pid/save" \
    "{\"productIdentity\":$ibody,\"yourRole\":$role_body}")" "200"
  check "role survives overall save" "$(get "/products/$pid" | jqp "['yourRole'].get('roleId')")" "$role"

  # Submit for QC
  local submitted
  submitted=$(post "/products/$pid/submit" '{}')
  if [[ "$(echo "$submitted" | jqp "['status']")" != "SUBMITTED_FOR_QC" ]]; then
    echo "    ↳ submit response: $(echo "$submitted" | head -c 400)"
  fi
  check "submit for QC" "$(echo "$submitted" | jqp "['status']")" "SUBMITTED_FOR_QC"
  check "statusKind for catalog" "$(echo "$submitted" | jqp "['statusKind']")" "qc-pending"

  # Editing after submit is blocked
  check "edit blocked after submit (409)" \
    "$(status_of PUT "/products/$pid/role" "{\"roleId\":\"$role\",\"data\":$rdata}")" "409"

  # QC queue contains it
  local inqueue
  inqueue=$(get "/products/qc-review?size=100" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('yes' if any(p['id']=='$pid' for p in d['content']) else 'no')")
  check "appears in QC queue" "$inqueue" "yes"

  # QC approve
  check "QC approve" "$(put "/products/$pid/qc-decision" \
    '{"decision":"APPROVE","reviewer":"qc.reviewer","remarks":"Looks good"}' | jqp "['status']")" "APPROVED"

}

echo "########################################################"
echo "# vendor-products-services E2E"
echo "# $BASE"
echo "########################################################"

# ---- health / metadata
check "catalog metadata reachable" "$(status_of GET "/catalog/categories")" "200"
check "5 categories declared" \
  "$(get "/catalog/categories" | jqp ".__len__()")" "5"
check "raw-materials has 9 type cards" \
  "$(get "/catalog/categories/raw-materials/identity-types" | jqp ".__len__()")" "9"
check "raw-materials has 7 role cards" \
  "$(get "/catalog/categories/raw-materials/roles" | jqp ".__len__()")" "7"

# ---- negative: required-field validation mirrors the UI
echo
echo "=== validation ==="
NPID=$(post "/products" '{"category":"raw-materials","identityType":"botanical-extract"}' | jqp "['id']")
check "incomplete identity rejected" \
  "$(status_of PUT "/products/$NPID/identity" '{"identityType":"botanical-extract","data":{"extractName":"X"}}')" "400"
check "error is keyed by form field name" \
  "$(put "/products/$NPID/identity" '{"identityType":"botanical-extract","data":{"extractName":"X"}}' \
     | jqp "['fieldErrors']['botanicalName']")" "Botanical Name is required"
check "draft save skips required checks" \
  "$(status_of PUT "/products/$NPID/identity" '{"identityType":"botanical-extract","data":{"extractName":"X"},"draft":true}')" "200"
check "unknown field rejected" \
  "$(status_of PUT "/products/$NPID/identity" '{"identityType":"botanical-extract","data":{"notAField":"x"},"draft":true}')" "400"
check "unknown type rejected" \
  "$(status_of PUT "/products/$NPID/identity" '{"identityType":"nonsense","data":{}}')" "400"
check "submit without variants rejected" \
  "$(status_of POST "/products/$NPID/submit" '{}')" "400"
check "unknown product 404" "$(status_of GET "/products/00000000-0000-0000-0000-000000000000")" "404"
# Regression: the wizard's select controls keep a pre-filled value that is not in
# the dropdown's option list (master search rows use "Bottles" where the dropdown
# offers "Bottle"), so option lists must be advisory, not enforced.
PKID=$(post "/products" '{"category":"packaging-materials"}' | jqp "['id']")
check "out-of-list select value accepted" \
  "$(status_of PUT "/products/$PKID/identity" '{"data":{"productName":"PET Bottle 500ml","commercialName":"PET Bottle","packagingCategory":"Rigid Packaging","packagingSubCategory":"Bottles","packagingFormat":"Round Bottles","primaryMaterialFamily":"PET","constructionType":"Blow Molded","countryOfOrigin":"India","commercialStage":"Commercially Available"},"draft":true}')" "200"

# ---- the five categories
run_category "Raw Materials (Botanical Extract)" "raw-materials" "botanical-extract" \
  '{"extractName":"Curcumin 95% Extract","botanicalName":"Curcuma longa","plantPartUsed":"Rhizome","extractType":"Standardized Extract","extractionMethod":"Solvent Extraction","markerCompound":"Curcumin","markerAssay":"≥ 95","standardizationType":"By HPLC","countryOfOrigin":"India","purityCurcuminoids":"≥ 95","commonTradeName":"Turmeric Extract","productDescription":"Standardized turmeric extract."}' \
  "manufacturer" \
  '{"selectFacilityCompany":"Verdant Biotics Manufacturing Facility - Goa, India","legalEntityName":"Verdant Biotics Pvt Ltd","gstin":"30AABCV1234B1Z5","iecCode":"0412345678","businessType":"Manufacturer"}'

run_category "Raw Materials (Raw Commodity)" "raw-materials" "raw-commodity" \
  '{"commodityName":"Wheat","botanicalName":"Triticum aestivum","plantPart":"Grain","cropOriginCountry":"India","moistureContent":"12.5","foreignMatter":"1.0","varietyCultivar":"HD 2967","cropYear":"2024-25"}' \
  "stockist" \
  '{"originalManufacturerName":"Natural Remedies Pvt Ltd","warehouseName":"Verdant Biotics Central Warehouse - Navi Mumbai","warehouseType":"Owned","leadTime":"1 - 3 Days"}'

run_category "Processing Machinery" "processing-machinery" "-" \
  '{"machineName":"Ribbon Blender RB-500","commercialModel":"RB-500 Series","brand":"MixTech","category":"Mixing & Blending Machines","subCategory":"Ribbon Blender","machineType":"Batch","functionProcess":"Mixing / Blending","shortDescription":"500 L ribbon blender.","detailedDescription":"Industrial ribbon blender for dry powder blending.","countryOfManufacture":"India","yearOfManufacture":"2025","baseModel":"RB-500","availabilityStatus":"Available","lifecycleStage":"Active","images":["data:image/png;base64,AAA"]}' \
  "manufacturer" '{"oemName":"MixTech","brandOwner":"MixTech","ipStatus":"Patented"}'

run_category "Finished Goods" "finished-goods" "-" \
  '{"productType":"Branded Product (Retail-Ready)","brandName":"HydraFit","commercialProductName":"HydraFit Electrolyte Drink","industry":"Beverage","sector":"Functional Beverages","category":"Ready-to-Drink (RTD)","commercialStage":"Commercial","countryOfOrigin":"USA","shortDescription":"Hydration drink.","detailedDescription":"Electrolyte beverage."}' \
  "brand-owner" '{"legalEntityName":"HydraFit Inc","authorizedToSell":true}'

run_category "Packaging Materials" "packaging-materials" "-" \
  '{"productName":"High Temperature Sterilization Bag","commercialName":"Retort Pouch 121/135","brand":"VerdaPack","packagingCategory":"Flexible Packaging","packagingSubCategory":"Retort Pouch","packagingFormat":"Stand-up Pouch","primaryMaterialFamily":"Multi-layer Laminate","constructionType":"Multi-layer Laminate","countryOfOrigin":"India","commercialStage":"Commercially Available","shortDescription":"Retort pouch.","detailedDescription":"Multi-layer retort pouch for 121/135C.","marketplaceImages":["data:image/png;base64,AAA"]}' \
  "manufacturer" '{"ownershipType":"Manufacturer","brandOwnerName":"VerdaPack","manufacturerName":"Verdant Biotics Pvt Ltd","authorizedToSell":true,"currency":"INR","listPrice":"12.5","moq":"10000","moqUnit":"pcs","leadTimeDays":"7-14","paymentTerms":"30% Advance / 70% Against Delivery","incoTerm":"FOB","sampleAvailable":true}'

run_category "Packaging Machinery" "packaging-machinery" "-" \
  '{"machineName":"Vertical FFS VFS-200","category":"Pouch Packing Machines","subCategory":"Vertical Form Fill Seal","machineType":"Automatic","baseModel":"VFS-200","brand":"PackTech","shortDescription":"VFFS machine.","longDescription":"Automatic vertical form fill seal machine for powders.","countryOfOrigin":"India"}' \
  "oem-manufacturer" '{"currency":"INR","listPrice":"1850000","moq":"1","moqUnit":"unit","incoTerm":"EXW","warrantyMonths":"12","authorizedToList":true}'

# ---- listing / filtering
echo
echo "=== listing ==="
check "list returns products" "$(get "/products?size=100" | python3 -c "
import sys,json; print('yes' if json.load(sys.stdin)['totalElements']>=6 else 'no')")" "yes"
check "category filter works" "$(get "/products?category=finished-goods&size=100" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('yes' if d['content'] and all(p['groupId']=='finished' for p in d['content']) else 'no')")" "yes"
check "search filter works" "$(get "/products?search=HydraFit&size=100" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('yes' if d['content'] and 'HydraFit' in d['content'][0]['name'] else 'no')")" "yes"
check "status filter accepts StatusKind" "$(status_of GET "/products?status=published&size=10")" "200"

echo
echo "########################################################"
echo "# PASS: $PASS   FAIL: $FAIL"
echo "########################################################"
[[ $FAIL -eq 0 ]]
