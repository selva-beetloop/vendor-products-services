package com.beetloop.vendorproducts.catalogue;

import com.beetloop.vendorproducts.persistence.SequenceGeneratorService;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Issues SCC / CM / VCG codes. Sequential per prefix, never reused.
 */
@Service
public class CatalogueIdService {

    public static final String SCC = "SCC";
    public static final String CM = "CM";
    public static final String VCG = "VCG";

    private final SequenceGeneratorService sequences;

    public CatalogueIdService(SequenceGeneratorService sequences) {
        this.sequences = sequences;
    }

    public String nextScientific(String materialToken) {
        return SCC + "-" + token(materialToken) + "-" + String.format("%03d", sequences.next("catalogue_" + SCC));
    }

    public String nextCommercial(String materialAssayToken) {
        return CM + "-" + token(materialAssayToken) + "-" + String.format("%03d", sequences.next("catalogue_" + CM));
    }

    public String listingCode(String commercialCode, String vendorId) {
        String body = commercialCode != null && commercialCode.startsWith("CM-")
                ? commercialCode.substring(3)
                : commercialCode;
        return VCG + "-" + body + "-" + vendorSuffix(vendorId);
    }

    public String vendorSuffix(String vendorId) {
        if (vendorId == null || vendorId.isBlank()) {
            return "V000";
        }
        String digits = vendorId.replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            int n = Integer.parseInt(digits.substring(Math.max(0, digits.length() - 3)));
            return String.format("V%03d", n);
        }
        return String.format("V%03d", Math.abs(vendorId.hashCode()) % 1000);
    }

    static String token(String raw) {
        if (raw == null || raw.isBlank()) {
            return "GEN";
        }
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return cleaned.isEmpty() ? "GEN" : cleaned.substring(0, Math.min(8, cleaned.length()));
    }
}
