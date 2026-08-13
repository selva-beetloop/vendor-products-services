package com.beetloop.catalog.facility.web;

import com.beetloop.catalog.facility.VendorFacility;
import com.beetloop.catalog.facility.VendorFacilityRepository;
import com.beetloop.catalog.shared.api.ApiResponse;
import com.beetloop.catalog.shared.error.ApiException;
import com.beetloop.catalog.shared.tenant.TenantContext;
import com.beetloop.catalog.shared.util.Ids;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Facilities")
@RestController
@RequestMapping("/vendor/facilities")
public class FacilityController {

    private final VendorFacilityRepository repository;

    public FacilityController(VendorFacilityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<VendorFacility>> list(@RequestParam(required = false) Boolean verified) {
        String vendorId = TenantContext.vendorId();
        return ApiResponse.of(verified == null
                ? repository.findByVendorId(vendorId)
                : repository.findByVendorIdAndVerified(vendorId, verified));
    }

    @GetMapping("/{facilityId}")
    public ApiResponse<VendorFacility> get(@PathVariable String facilityId) {
        return ApiResponse.of(repository.findByIdAndVendorId(facilityId, TenantContext.vendorId())
                .orElseThrow(() -> ApiException.notFound("Facility " + facilityId)));
    }

    /** The "Add New Facility" action. `verified` stays false until the onboarding team confirms it. */
    @PostMapping
    public ResponseEntity<ApiResponse<VendorFacility>> create(@RequestBody VendorFacility request) {
        VendorFacility facility = VendorFacility.builder()
                .id(Ids.newId("fac"))
                .vendorId(TenantContext.vendorId())
                .name(request.getName())
                .address(request.getAddress())
                .country(request.getCountry())
                .facilityType(request.getFacilityType())
                .gmpCertified(request.isGmpCertified())
                .certifications(request.getCertifications())
                .verified(false)
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.status(201).body(ApiResponse.of(repository.save(facility)));
    }
}
