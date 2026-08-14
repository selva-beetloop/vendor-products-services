package com.beetloop.vendorproducts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full variant payload — all five "Add Variant" sub-steps in one object, matching
 * the shape the wizard assembles before the vendor clicks "Add Variant".
 *
 * <p>Every sub-step is optional so the same DTO backs both the bundled
 * {@code POST /variants} call and the individual sub-step endpoints.
 */
@Schema(description = "One variant / pack size / model with all five sub-steps.")
public record VariantRequest(

        VariantDetails variantDetails,
        TechnicalSpecifications technicalSpecifications,
        CommercialPricingDto commercialPricing,
        ComplianceCertifications complianceCertifications,
        Map<String, Object> searchMarketplace,
        Boolean draft) {

    public boolean isDraft() {
        return Boolean.TRUE.equals(draft);
    }

    /** Sub-step 3.1. */
    @Schema(description = "Variant Details sub-step.")
    public record VariantDetails(
            String name,
            String variantType,
            String grade,
            String assayPurity,
            String packSize,
            String packagingType,
            String particleSize,
            String skuCode,
            String batchPrefix,
            String status,
            List<String> images,
            @Schema(description = "Category-specific variant fields (flavor, gtin, thicknessUm, capacity, …)")
            Map<String, Object> extra) {

        public List<String> imagesOrEmpty() {
            return images == null ? new ArrayList<>() : images;
        }

        public Map<String, Object> extraOrEmpty() {
            return extra == null ? new LinkedHashMap<>() : extra;
        }
    }

    /** Sub-step 3.2 — repeatable specification groups, each with repeatable parameters. */
    @Schema(description = "Technical Specifications sub-step.")
    public record TechnicalSpecifications(List<SpecificationGroupDto> data) {

        public List<SpecificationGroupDto> dataOrEmpty() {
            return data == null ? new ArrayList<>() : data;
        }
    }

    @Schema(description = "One 'Add Specification' block.")
    public record SpecificationGroupDto(
            String title,
            String tag,
            Boolean collapsed,
            List<SpecificationParameterDto> data) {

        public List<SpecificationParameterDto> dataOrEmpty() {
            return data == null ? new ArrayList<>() : data;
        }
    }

    @Schema(description = "One 'Add Parameter' row.")
    public record SpecificationParameterDto(
            String parameterName,
            String specification,
            String unit,
            String testMethodOrStandard,
            String requirementSource,
            String attachment,
            String priority,
            String remarks) {
    }

    /** Sub-step 3.3 — all four Commercial &amp; Pricing tabs. */
    @Schema(description = "Commercial & Pricing sub-step (four tabs).")
    public record CommercialPricingDto(
            PricingQuantity pricingQuantity,
            CommercialTradeTerms commercialTradeTerms,
            PackagingAndSamples packagingAndSamples,
            IncludedDocumentsAndServices includedDocumentsAndServices) {
    }

    @Schema(description = "Tab 1 — Pricing & Quantity.")
    public record PricingQuantity(
            BasePricing basePricing,
            List<PriceTierDto> volumePricing,
            CommercialCharges commercialCharges) {

        public List<PriceTierDto> volumePricingOrEmpty() {
            return volumePricing == null ? new ArrayList<>() : volumePricing;
        }
    }

    public record BasePricing(BigDecimal pricePerUnit, String unit, String moq, String leadTime) {
    }

    public record PriceTierDto(
            String quantityRange,
            String tierName,
            String pricePerUnit,
            String discountVsBase,
            String leadTime) {
    }

    public record CommercialCharges(
            String freightCharges,
            String insuranceCharges,
            String handlingCharges,
            String otherCharges) {
    }

    @Schema(description = "Tab 2 — Commercial & Trade Terms.")
    public record CommercialTradeTerms(
            String currency,
            String paymentTerms,
            String incoterms,
            String priceValidityDays,
            String gstTaxes,
            Boolean exportAvailable,
            String minOrderValue,
            Boolean partialShipmentAllowed,
            String returnPolicy,
            String warrantyPeriod) {
    }

    @Schema(description = "Tab 3 — Packaging & Samples.")
    public record PackagingAndSamples(
            List<PackagingOptionDto> packaging,
            SampleInformation sampleInformation) {

        public List<PackagingOptionDto> packagingOrEmpty() {
            return packaging == null ? new ArrayList<>() : packaging;
        }
    }

    public record PackagingOptionDto(String packagingType, String size, String customPackaging) {
    }

    public record SampleInformation(
            Boolean sampleAvailable,
            String freePaidSample,
            String sampleCost,
            String sampleTurnaroundDays,
            String maxSampleQty,
            String sampleShippingBorneBy) {
    }

    @Schema(description = "Tab 4 — Included Documents & Services (checkbox groups).")
    public record IncludedDocumentsAndServices(
            List<String> documentsIncluded,
            List<String> servicesIncluded) {

        public List<String> documentsOrEmpty() {
            return documentsIncluded == null ? new ArrayList<>() : documentsIncluded;
        }

        public List<String> servicesOrEmpty() {
            return servicesIncluded == null ? new ArrayList<>() : servicesIncluded;
        }
    }

    /** Sub-step 3.4. */
    @Schema(description = "Compliance & Certifications sub-step.")
    public record ComplianceCertifications(List<ComplianceDocumentDto> data) {

        public List<ComplianceDocumentDto> dataOrEmpty() {
            return data == null ? new ArrayList<>() : data;
        }
    }

    @Schema(description = "One row from the 'Add Certificate / Document' modal.")
    public record ComplianceDocumentDto(
            String category,
            String name,
            String reference,
            String authority,
            String applicableTo,
            String date,
            String expiryDate,
            String status,
            String fileName,
            String fileId,
            String fileUrl) {
    }
}
