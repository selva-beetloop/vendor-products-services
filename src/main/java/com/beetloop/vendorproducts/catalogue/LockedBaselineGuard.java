package com.beetloop.vendorproducts.catalogue;

import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rejects vendor identity payloads that mutate locked T1/T2 baseline keys.
 */
@Component
public class LockedBaselineGuard {

    static final Set<String> LOCKED_KEYS = Set.of(
            "casnumber", "casno", "cas", "formula", "mw", "molecularweight",
            "botanicalname", "assay", "assaypurity", "markerassay", "puritycurcuminoids",
            "grade", "form", "physicalform", "origin", "countryoforigin", "croporigincountry",
            "colour", "color", "source", "regulatoryclass");

    public void rejectIfBaselineMutated(CommercialMaster t2, Map<String, Object> incoming) {
        if (t2 == null || incoming == null || incoming.isEmpty()) {
            return;
        }
        Map<String, Object> baseline = t2.getBaseline();
        ValidationException.Builder errors = new ValidationException.Builder();
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            if (!isLocked(entry.getKey())) {
                continue;
            }
            Object expected = lookup(baseline, entry.getKey());
            if (expected == null) {
                expected = columnFallback(t2, entry.getKey());
            }
            if (expected == null) {
                continue;
            }
            if (!same(expected, entry.getValue())) {
                errors.add(entry.getKey(),
                        "Locked baseline field '" + entry.getKey() + "' cannot be changed. Branch to a new T2 instead.",
                        entry.getValue());
            }
        }
        errors.throwIfAny("Locked baseline cannot be edited");
    }

    public boolean isGradeDefiningChange(CommercialMaster t2, Map<String, Object> incoming) {
        if (t2 == null || incoming == null) {
            return false;
        }
        return changed(incoming, "assay", t2.getAssay())
                || changed(incoming, "assayPurity", t2.getAssay())
                || changed(incoming, "grade", t2.getGrade())
                || changed(incoming, "form", t2.getForm())
                || changed(incoming, "physicalForm", t2.getForm())
                || changed(incoming, "origin", t2.getOrigin())
                || changed(incoming, "countryOfOrigin", t2.getOrigin())
                || changed(incoming, "colour", t2.getColour())
                || changed(incoming, "source", t2.getSource());
    }

    private boolean changed(Map<String, Object> incoming, String key, String current) {
        if (!incoming.containsKey(key) || current == null || current.isBlank()) {
            return false;
        }
        return !same(current, incoming.get(key));
    }

    private static boolean isLocked(String key) {
        return key != null && LOCKED_KEYS.contains(key.toLowerCase(Locale.ROOT).replace("_", ""));
    }

    private static Object lookup(Map<String, Object> baseline, String key) {
        if (baseline == null) {
            return null;
        }
        if (baseline.containsKey(key)) {
            return baseline.get(key);
        }
        String needle = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : baseline.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(needle)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Object columnFallback(CommercialMaster t2, String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return switch (k) {
            case "assay", "assaypurity", "markerassay" -> t2.getAssay();
            case "grade" -> t2.getGrade();
            case "form", "physicalform" -> t2.getForm();
            case "origin", "countryoforigin" -> t2.getOrigin();
            case "colour", "color" -> t2.getColour();
            case "source" -> t2.getSource();
            default -> null;
        };
    }

    private static boolean same(Object expected, Object actual) {
        if (actual == null || String.valueOf(actual).isBlank()) {
            return true;
        }
        return CommercialMaster.normalize(String.valueOf(expected))
                .equals(CommercialMaster.normalize(String.valueOf(actual)));
    }
}
