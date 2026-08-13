package com.beetloop.catalog.facility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Source of the read-only panel that auto-populates when a vendor picks "Select Facility / Company".
 * The listing PROJECTS this; a client write to facilitySnapshot is rejected.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendor_facilities")
public class VendorFacility {

    @Id
    private String id;

    private String vendorId;
    private String name;
    private String address;
    private String country;
    private String facilityType;
    private boolean gmpCertified;
    private List<String> certifications;

    /** Set by the onboarding team, never by the vendor. */
    private boolean verified;
    private Instant verifiedAt;

    private Instant createdAt;
}
