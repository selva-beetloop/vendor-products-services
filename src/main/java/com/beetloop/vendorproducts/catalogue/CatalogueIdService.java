package com.beetloop.vendorproducts.catalogue;

import com.beetloop.vendorproducts.pm.domain.PmIdSequence;
import com.beetloop.vendorproducts.pm.repository.PmIdSequenceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Issues SCC / CM / VCG codes. Sequential per prefix, never reused.
 */
@Service
public class CatalogueIdService {

    public static final String SCC = "SCC";
    public static final String CM = "CM";
    public static final String VCG = "VCG";

    private final PmIdSequenceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public CatalogueIdService(PmIdSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextScientific(String materialToken) {
        return SCC + "-" + token(materialToken) + "-" + String.format("%03d", nextNumber(SCC));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextCommercial(String materialAssayToken) {
        return CM + "-" + token(materialAssayToken) + "-" + String.format("%03d", nextNumber(CM));
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

    private long nextNumber(String prefix) {
        int year = 0;
        PmIdSequence sequence = repository.lock(prefix, year);
        if (sequence == null) {
            sequence = repository.save(new PmIdSequence(prefix, year));
            entityManager.flush();
        }
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        repository.save(sequence);
        return value;
    }

    static String token(String raw) {
        if (raw == null || raw.isBlank()) {
            return "GEN";
        }
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return cleaned.isEmpty() ? "GEN" : cleaned.substring(0, Math.min(8, cleaned.length()));
    }
}
