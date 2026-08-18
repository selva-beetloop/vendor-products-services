package com.beetloop.vendorproducts.controller;

import com.beetloop.vendorproducts.dto.CreateProductRequest;
import com.beetloop.vendorproducts.dto.ProductResponse;
import com.beetloop.vendorproducts.security.CurrentUser;
import com.beetloop.vendorproducts.service.VendorProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** BRD Flow A — create T3 from a live T2. */
@RestController
@RequestMapping("/api/vendor/listings")
@Tag(name = "Vendor Listings", description = "T3 create from an approved commercial master.")
public class VendorListingController {

    private final VendorProductService products;
    private final CurrentUser currentUser;

    public VendorListingController(VendorProductService products, CurrentUser currentUser) {
        this.products = products;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Create a T3 listing from a live T2")
    public ResponseEntity<ProductResponse> create(@RequestBody CreateProductRequest request) {
        String userId = currentUser.userId();
        ProductResponse created = products.create(request, userId, userId);
        return ResponseEntity.created(URI.create("/api/vendor/products/" + created.id())).body(created);
    }
}
