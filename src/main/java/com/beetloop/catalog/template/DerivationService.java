package com.beetloop.catalog.template;

import com.beetloop.catalog.config.CatalogProperties;
import com.beetloop.catalog.shared.util.DateNormalizer;
import com.beetloop.catalog.shared.util.Ids;
import com.beetloop.catalog.shared.util.Maps;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single writer of every derived value. Having one owner is what makes "reject client values
 * for derived fields" enforceable rather than aspirational.
 *
 * Everything here appears in document 02 section 15: certificate status, overallCompletion,
 * counters, analysisSummary, variantCodeSku, totalThicknessMicron, occupancyPct.
 */
@Service
public class DerivationService {

    private final DateNormalizer dateNormalizer;
    private final CatalogProperties properties;

    public DerivationService(DateNormalizer dateNormalizer, CatalogProperties properties) {
        this.dateNormalizer = dateNormalizer;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ certificates

    /**
     * The certificate modal offers Active / Expiring / Expired to the vendor, but status is a
     * function of expiryDate against server time. The running UI renders "Complete" for
     * accreditations that expired in 2023 - so a client-supplied status is dropped, not merged.
     */
    public void deriveCertificateRows(Map<String, Object> section) {
        List<Map<String, Object>> rows = Maps.asMapList(section.get("data"));
        for (Map<String, Object> row : rows) {
            row.putIfAbsent("certificateId", Ids.newId("cert"));
            Map<String, Object> interpretation = new LinkedHashMap<>();
            LocalDate expiry = normalizeInto(row, "date", interpretation);
            expiry = normalizeInto(row, "expiryDate", interpretation);
            if (!interpretation.isEmpty()) {
                row.put("dateInterpretation", interpretation);
            }
            row.put("status", expiryStatus(expiry));
            if (expiry != null && expiry.isBefore(LocalDate.now())) {
                row.put("daysExpired", ChronoUnit.DAYS.between(expiry, LocalDate.now()));
            } else {
                row.remove("daysExpired");
            }
        }
        section.put("pagination", pagination(rows.size()));
    }

    private LocalDate normalizeInto(Map<String, Object> row, String key, Map<String, Object> interpretation) {
        DateNormalizer.Normalized normalized = dateNormalizer.normalize(row.get(key));
        if (normalized == null) {
            return null;
        }
        row.put(key, normalized.value().toString());
        interpretation.put(key, normalized.pattern());
        return normalized.value();
    }

    public String expiryStatus(LocalDate expiryDate) {
        if (expiryDate == null) {
            return "VALID";
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        long days = ChronoUnit.DAYS.between(today, expiryDate);
        return days <= properties.getDocuments().getExpiringSoonWindowDays() ? "EXPIRING_SOON" : "VALID";
    }

    // ------------------------------------------------------------------ specification groups

    /**
     * The two-level engine: specification groups -> parameter rows. Reused by every product
     * category's technical-specification stage and by Lab Testing sub-step 3.
     */
    public void deriveSpecificationGroups(Map<String, Object> section, String rowsKey) {
        Map<String, Object> holder = section;
        if (rowsKey != null) {
            Map<String, Object> nested = Maps.mapAt(section, rowsKey);
            if (nested == null) {
                return;
            }
            holder = nested;
        }
        List<Map<String, Object>> groups = Maps.asMapList(holder.get("data"));
        int completedTotal = 0;
        int total = 0;
        for (Map<String, Object> group : groups) {
            group.putIfAbsent("specificationId", Ids.newId("spec"));
            group.putIfAbsent("badge", "PRIMARY");
            List<Map<String, Object>> params = Maps.asMapList(group.get("data"));
            int completed = 0;
            for (Map<String, Object> param : params) {
                param.putIfAbsent("parameterId", Ids.newId("p"));
                if (isParameterComplete(param)) {
                    completed++;
                }
            }
            int groupTotal = Math.max(params.size(), intOf(group.get("total")));
            group.put("completed", completed);
            group.put("total", groupTotal);
            completedTotal += completed;
            total += groupTotal;
        }
        holder.put("overallCompletion", Map.of(
                "completed", completedTotal,
                "total", total,
                "percent", percent(completedTotal, total)));
    }

    private boolean isParameterComplete(Map<String, Object> param) {
        // A row counts once it carries an identity and a value, whichever column set the category uses.
        boolean named = !Maps.isBlank(param.get("parameterName")) || !Maps.isBlank(param.get("parameterAnalyte"));
        boolean valued = !Maps.isBlank(param.get("specification")) || !Maps.isBlank(param.get("measuringRange"))
                || !Maps.isBlank(param.get("lodTypical"));
        return named && valued;
    }

    /** Lab Testing sub-step 3.2 - every figure on the Analysis Summary panel. */
    public void deriveAnalysisSummary(Map<String, Object> section, Map<String, Object> pricingSection) {
        Map<String, Object> specs = Maps.mapAt(section, "analysisSpecifications");
        if (specs == null) {
            return;
        }
        List<Map<String, Object>> groups = Maps.asMapList(specs.get("data"));
        int totalParameters = 0;
        int accredited = 0;
        for (Map<String, Object> group : groups) {
            for (Map<String, Object> param : Maps.asMapList(group.get("data"))) {
                totalParameters++;
                if ("ACCREDITED".equals(param.get("status"))) {
                    accredited++;
                }
            }
        }
        int repeats = 0;
        BigDecimal price = null;
        if (pricingSection != null) {
            Map<String, Object> repeatOptions = Maps.mapAt(pricingSection, "repeatAnalysisOptions");
            if (repeatOptions != null) {
                repeats = intOf(repeatOptions.get("maxRepeatAnalysis"));
            }
            Map<String, Object> pricing = Maps.mapAt(pricingSection, "pricingCommercialDetails");
            if (pricing != null && pricing.get("price") instanceof Number n) {
                price = new BigDecimal(n.toString());
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalParameters", totalParameters);
        summary.put("totalRepeatAnalysis", repeats);
        summary.put("totalAnalysesCalculated", totalParameters * Math.max(repeats, 1));
        summary.put("accreditedParameters", accredited);
        summary.put("nonAccredited", totalParameters - accredited);
        summary.put("estimatedCostPerSample", price);
        summary.put("estimatedReportPages", estimatePages(totalParameters));
        section.put("analysisSummary", summary);
    }

    private String estimatePages(int parameters) {
        int low = Math.max(1, (int) Math.ceil(parameters / 2.0));
        return low + "-" + (low + 1);
    }

    // ------------------------------------------------------------------ category-specific

    /** "Auto-generated from base model, capacity and automation level." */
    public String variantCodeSku(Map<String, Object> variantDetails) {
        String base = Maps.str(variantDetails, "baseModelSeries");
        Map<String, Object> capacity = Maps.mapAt(variantDetails, "capacityWorkingVolume");
        if (capacity == null) {
            capacity = Maps.mapAt(variantDetails, "capacityOutput");
        }
        String automation = Maps.str(variantDetails, "automationLevel");
        if (base == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(slug(base));
        if (capacity != null && capacity.get("value") != null) {
            sb.append('-').append(trimDecimal(capacity.get("value")));
        }
        if (automation != null) {
            sb.append('-').append(initials(automation));
        }
        return sb.toString();
    }

    /** Packaging Materials: the construction layers must sum to the stated total. */
    public void deriveConstructionTotals(Map<String, Object> section) {
        List<Map<String, Object>> layers = Maps.asMapList(section.get("layers"));
        if (layers.isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> layer : layers) {
            if (layer.get("thicknessMicron") instanceof Number n) {
                total = total.add(new BigDecimal(n.toString()));
            }
        }
        section.put("totalThicknessMicron", total);
    }

    /** Agro Cluster, zone/plot-defined providers. */
    public void deriveOccupancy(Map<String, Object> zonePlotDefined) {
        int plots = intOf(zonePlotDefined.get("numberOfPlots"));
        int allotted = intOf(zonePlotDefined.get("plotsAllotted"));
        if (plots > 0) {
            zonePlotDefined.put("occupancyPct",
                    BigDecimal.valueOf(allotted * 100.0 / plots).setScale(1, java.math.RoundingMode.HALF_UP));
        }
    }

    // ------------------------------------------------------------------ counters

    /** The Raw Materials tab counts are DISTINCT VALUES per axis, which is why they do not sum to the total. */
    public Map<String, Object> variantCounters(List<Map<String, Object>> variantDetailsList) {
        Set<String> grades = new LinkedHashSet<>();
        Set<String> packSizes = new LinkedHashSet<>();
        Set<String> assays = new LinkedHashSet<>();
        Set<String> particleSizes = new LinkedHashSet<>();
        Set<String> combinations = new LinkedHashSet<>();
        for (Map<String, Object> details : variantDetailsList) {
            addIfPresent(grades, details.get("grade"));
            addIfPresent(packSizes, details.get("packSize"));
            addIfPresent(assays, details.get("assayPurity"));
            addIfPresent(particleSizes, details.get("particleSizeMesh"));
            if ("COMBINATION".equals(details.get("variantType"))) {
                addIfPresent(combinations, details.get("variantName"));
            }
        }
        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("grades", grades.size());
        counters.put("packSizes", packSizes.size());
        counters.put("assayLevels", assays.size());
        counters.put("particleSizes", particleSizes.size());
        counters.put("total", variantDetailsList.size());
        return counters;
    }

    public Map<String, Object> variantGroupings(List<Map<String, Object>> variantDetailsList) {
        Map<String, Object> counters = variantCounters(variantDetailsList);
        Map<String, Object> groupings = new LinkedHashMap<>();
        groupings.put("byGrade", counters.get("grades"));
        groupings.put("byPackSize", counters.get("packSizes"));
        groupings.put("byAssay", counters.get("assayLevels"));
        groupings.put("byParticleSize", counters.get("particleSizes"));
        groupings.put("byCombination", variantDetailsList.stream()
                .filter(d -> "COMBINATION".equals(d.get("variantType"))).count());
        return groupings;
    }

    public Map<String, Object> pagination(int totalElements) {
        int size = 10;
        return Map.of(
                "page", 0,
                "size", size,
                "totalElements", totalElements,
                "totalPages", (int) Math.ceil(totalElements / (double) size));
    }

    public int percent(int completed, int total) {
        return total <= 0 ? 0 : (int) Math.round(completed * 100.0 / total);
    }

    // ------------------------------------------------------------------ small helpers

    private void addIfPresent(Set<String> target, Object value) {
        if (!Maps.isBlank(value)) {
            target.add(String.valueOf(value).trim());
        }
    }

    private int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private String trimDecimal(Object value) {
        String s = String.valueOf(value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private String slug(String value) {
        return value.toUpperCase().replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String initials(String value) {
        StringBuilder sb = new StringBuilder();
        for (String part : value.split("[^A-Za-z0-9]+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }

    public List<Map<String, Object>> emptyRows() {
        return new ArrayList<>();
    }
}
