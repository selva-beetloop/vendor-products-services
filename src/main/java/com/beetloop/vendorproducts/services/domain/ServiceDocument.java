package com.beetloop.vendorproducts.services.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An accreditation, certification or support document attached to a service —
 * the "Add Accreditation" / "Add Certification" / "Add Support Doc" modals and
 * the "Manage Documents" list.
 *
 * <p>The three kinds share this table because they share a lifecycle and a
 * management screen, but their field sets differ per kind <em>and</em> per
 * category, so the fields live in {@link #data} and are validated against the
 * registry. Notably not every category offers all three:
 *
 * <ul>
 *   <li>Lab Testing, CRO, Agro-Processing — all three kinds</li>
 *   <li>Contract Manufacturer — certification and support doc only, no accreditation</li>
 *   <li>Consultancy — none of the three</li>
 * </ul>
 *
 * so a uniform sub-resource would have been wrong.
 */
public class ServiceDocument {

    /** Which modal produced the record. */
    public enum Kind {
        ACCREDITATION,
        CERTIFICATION,
        SUPPORT_DOC;

        public static Kind from(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String needle = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            for (Kind kind : values()) {
                if (kind.name().equals(needle)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown document kind '" + raw + "'. Expected ACCREDITATION, CERTIFICATION or SUPPORT_DOC");
        }
    }

    @Id
    private UUID id;

    @Transient
    private VendorService service;

    private int position;

    private Kind kind;

    // ---- columns common to all three kinds, so the Manage Documents list can
    // ---- render without unpacking the JSON ----

    /**
     * The client's own id for this document (a credential row id in the wizard).
     *
     * <p>Carried so a reload can match a stored document back to the row that
     * created it. Without it the client can only guess by comparing reference
     * numbers, and an edit to a reference number breaks the link.
     */
    private String externalRef;

    private String name;

    private String issuingBody;

    private String referenceNumber;

    private String validFrom;

    private String validTo;

    private String status;

    private String fileName;

    private String fileId;

    private String fileUrl;

    /** The remaining kind- and category-specific fields. */
    private Map<String, Object> data = new LinkedHashMap<>();

    private Instant createdAt;

    private Instant updatedAt;

    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---- getters / setters ----

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VendorService getService() {
        return service;
    }

    public void setService(VendorService service) {
        this.service = service;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public void setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
