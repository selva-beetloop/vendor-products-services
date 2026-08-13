package com.beetloop.catalog.product;

import com.beetloop.catalog.product.dto.ProductDtos;
import com.beetloop.catalog.product.model.ProductListing;
import com.beetloop.catalog.product.model.ProductVariant;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDtos.ProductResponse toResponse(ProductListing listing) {
        return new ProductDtos.ProductResponse(
                listing.getId(),
                listing.getCode(),
                listing.getCategoryCode(),
                listing.getCategoryId(),
                listing.getEntryPath(),
                listing.getMasterProductId(),
                listing.getRequestCode(),
                listing.getTemplateVersion(),
                listing.getCurrentStep(),
                listing.getCompletedSteps(),
                listing.getData(),
                listing.getVariants().stream().map(ProductMapper::toVariantResponse).toList(),
                listing.getDerived(),
                listing.getQcStatus(),
                listing.getLifecycle(),
                listing.versionOrZero(),
                ProductGuard.etag(listing),
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }

    public static ProductDtos.VariantResponse toVariantResponse(ProductVariant variant) {
        return new ProductDtos.VariantResponse(
                variant.getVariantId(),
                variant.getSections(),
                variant.getStatus(),
                variant.getCompletionPercent(),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    public static List<ProductDtos.VariantResponse> toVariantResponses(List<ProductVariant> variants) {
        return variants.stream().map(ProductMapper::toVariantResponse).toList();
    }
}
