package com.beetloop.vendorproducts.pm.service;

import com.beetloop.vendorproducts.persistence.SequenceGeneratorService;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * Issues the business IDs defined in BRD §12.
 *
 * <p>Format: <code>{ENTITY PREFIX}-{YYYY}-{NNNN}</code> (e.g. {@code PRJ-2026-0001}),
 * sequential per entity per year. BOMs, batches and trials additionally carry a
 * version suffix — see {@link #withVersion(String, int)} — where the base number
 * is preserved and {@code V1}, {@code V2}… appended, so history and material
 * reuse stay traceable (§12.3).
 */
@Service
public class PmIdService {

    /** Entity prefixes from the BRD §12.2 ID matrix. */
    public static final String PROJECT = "PRJ";
    public static final String LINE_ITEM = "PLI";
    public static final String ORDER = "ORD";
    public static final String STAGE = "STG";
    public static final String TASK = "TSK";
    public static final String CHECKLIST = "CHK";
    public static final String DEPENDENCY = "DEP";
    public static final String CHANGE_ORDER = "CO";
    public static final String APPROVAL = "APR";
    public static final String QUERY = "QRY";
    public static final String ISSUE = "ISS";
    public static final String ESCALATION = "ESC";
    public static final String DELIVERABLE = "DLV";
    public static final String SHIPMENT = "SHP";
    public static final String DELIVERY_FEEDBACK = "DFB";
    public static final String STOCK_CHECK = "STK";

    private final SequenceGeneratorService sequences;

    public PmIdService(SequenceGeneratorService sequences) {
        this.sequences = sequences;
    }

    /**
     * Reserves the next id for a prefix. Uses an atomic Mongo counter so concurrent
     * creates cannot be handed the same number.
     */
    public String next(String prefix) {
        int year = Year.now().getValue();
        long value = sequences.next("pm_" + prefix + "_" + year);
        return format(prefix, year, value);
    }

    /** BRD §12.1 — stage numbers are positional within an order (STG-01, STG-02…). */
    public String stageNumber(int position) {
        return String.format("%s-%02d", STAGE, position + 1);
    }

    /** Appends the rework version suffix, keeping the base number (§12.3). */
    public String withVersion(String baseId, int version) {
        return baseId + "-V" + version;
    }

    static String format(String prefix, int year, long value) {
        return String.format("%s-%d-%04d", prefix, year, value);
    }
}
