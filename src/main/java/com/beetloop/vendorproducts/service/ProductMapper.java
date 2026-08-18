package com.beetloop.vendorproducts.service;

import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.repository.CommercialMasterRepository;
import com.beetloop.vendorproducts.domain.CommercialPricing;
import com.beetloop.vendorproducts.domain.ProductVariant;
import com.beetloop.vendorproducts.domain.VariantComplianceDocument;
import com.beetloop.vendorproducts.domain.VariantPackagingOption;
import com.beetloop.vendorproducts.domain.VariantPriceTier;
import com.beetloop.vendorproducts.domain.VariantSpecificationGroup;
import com.beetloop.vendorproducts.domain.VariantSpecificationParameter;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.dto.ProductResponse;
import com.beetloop.vendorproducts.dto.ProductSummaryResponse;
import com.beetloop.vendorproducts.dto.VariantRequest;
import com.beetloop.vendorproducts.dto.VariantResponse;
import com.beetloop.vendorproducts.storage.DataUriPersister;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Entity ↔ DTO translation. Response shapes intentionally mirror the wizard's own state objects. */
@Component
public class ProductMapper {

    private final CommercialMasterRepository commercialMasters;
    private final DataUriPersister dataUriPersister;

    public ProductMapper(CommercialMasterRepository commercialMasters, DataUriPersister dataUriPersister) {
        this.commercialMasters = commercialMasters;
        this.dataUriPersister = dataUriPersister;
    }

    private static final Map<String, String> FLAG_BY_COUNTRY = Map.ofEntries(
            Map.entry("india", "🇮🇳"),
            Map.entry("usa", "🇺🇸"),
            Map.entry("united states", "🇺🇸"),
            Map.entry("china", "🇨🇳"),
            Map.entry("germany", "🇩🇪"),
            Map.entry("italy", "🇮🇹"),
            Map.entry("japan", "🇯🇵"),
            Map.entry("south korea", "🇰🇷"),
            Map.entry("united kingdom", "🇬🇧"),
            Map.entry("uk", "🇬🇧"),
            Map.entry("uae", "🇦🇪"),
            Map.entry("vietnam", "🇻🇳"),
            Map.entry("canada", "🇨🇦"),
            Map.entry("australia", "🇦🇺"),
            Map.entry("switzerland", "🇨🇭"),
            Map.entry("brazil", "🇧🇷"));

    // ---- product ----

    public ProductResponse toResponse(VendorProduct product) {
        List<VariantResponse> variants = product.getVariants().stream()
                .map(this::toVariantResponse)
                .toList();

        boolean identityDone = product.getIdentityPayload() != null && !product.getIdentityPayload().isEmpty();
        boolean roleDone = product.getRoleId() != null && !product.getRoleId().isBlank();

        CommercialMaster t2 = resolveCommercial(product);

        return new ProductResponse(
                product.getId().toString(),
                product.getListingCode(),
                product.getListingCode(),
                product.getCategory().getId(),
                product.getCategory().getGroupId(),
                product.getStatus().name(),
                product.getStatus().getStatusKind(),
                product.getStatus().getStatusLabel(),
                product.getName(),
                product.getSku(),
                product.getListingCategory(),
                product.getOriginCountry(),
                product.getThumbEmoji(),
                product.getThumbImage(),
                product.isVerified(),
                product.getSourceMasterId(),
                product.getCommercialMasterId() == null ? null : product.getCommercialMasterId().toString(),
                t2 == null ? product.getSourceMasterId() : t2.getCode(),
                t2 == null || t2.getScientificMaster() == null ? null : t2.getScientificMaster().getId().toString(),
                t2 == null || t2.getScientificMaster() == null ? null : t2.getScientificMaster().getCode(),
                product.isHoldPublish(),
                new ProductResponse.IdentitySection(product.getIdentityType(),
                        product.getIdentityPayload() == null ? Map.of() : product.getIdentityPayload()),
                new ProductResponse.RoleSection(product.getRoleId(),
                        product.getRolePayload() == null ? Map.of() : product.getRolePayload()),
                variants,
                new ProductResponse.QcSection(product.getQcReviewer(), product.getQcRemarks(),
                        product.getSubmittedAt(), product.getReviewedAt()),
                new ProductResponse.Completion(identityDone, roleDone, !variants.isEmpty(),
                        product.getStatus().isSubmitted()),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    public ProductSummaryResponse toSummary(VendorProduct product) {
        String country = product.getOriginCountry();
        String flag = country == null ? "🏳️"
                : FLAG_BY_COUNTRY.getOrDefault(country.trim().toLowerCase(), "🏳️");

        List<String> documents = new ArrayList<>();
        int documentCount = 0;
        for (ProductVariant variant : product.getVariants()) {
            for (VariantComplianceDocument document : variant.getComplianceDocuments()) {
                documentCount++;
                if (documents.size() < 3 && document.getName() != null && !documents.contains(document.getName())) {
                    documents.add(document.getName());
                }
            }
        }

        boolean sampleAvailable = product.getVariants().stream()
                .anyMatch(v -> Boolean.TRUE.equals(v.getCommercialPricing().getSampleAvailable()));

        return new ProductSummaryResponse(
                product.getId().toString(),
                product.getName(),
                product.getSku(),
                true,
                product.isVerified(),
                product.getCategory().getTypeLabel(),
                product.getCategory().getGroupId(),
                product.getListingCategory(),
                product.getRoleId() == null ? List.of() : List.of(product.getRoleId()),
                List.of(),
                flag,
                country,
                documents,
                Math.max(0, documentCount - documents.size()),
                sampleAvailable ? "available" : "on-request",
                "—",
                "—",
                product.getStatus().getStatusKind(),
                product.getStatus().getStatusLabel(),
                product.getStatus().name(),
                product.getStatus().isSubmitted() ? "eye" : "edit",
                product.getThumbEmoji(),
                product.getThumbImage(),
                product.getVariants().size(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    // ---- variant: entity → DTO ----

    public VariantResponse toVariantResponse(ProductVariant variant) {
        CommercialPricing pricing = variant.getCommercialPricing();

        List<VariantRequest.SpecificationGroupDto> groups = variant.getSpecificationGroups().stream()
                .map(group -> new VariantRequest.SpecificationGroupDto(
                        group.getTitle(),
                        group.getTag(),
                        group.isCollapsed(),
                        group.getParameters().stream()
                                .map(p -> new VariantRequest.SpecificationParameterDto(
                                        p.getParameter(), p.getSpecification(), p.getUnit(),
                                        p.getMethod(), p.getSource(), p.getAttachment(),
                                        p.getPriority(), p.getRemarks()))
                                .toList()))
                .toList();

        List<VariantRequest.PriceTierDto> tiers = variant.getPriceTiers().stream()
                .map(t -> new VariantRequest.PriceTierDto(t.getQtyBreak(), t.getTierLabel(),
                        t.getPricePerUnit(), t.getDiscountVsBase(), t.getLeadTimeDays()))
                .toList();

        List<VariantRequest.PackagingOptionDto> packaging = variant.getPackagingOptions().stream()
                .map(o -> new VariantRequest.PackagingOptionDto(o.getPackagingType(), o.getSize(),
                        o.getCustomPackaging()))
                .toList();

        List<VariantRequest.ComplianceDocumentDto> documents = variant.getComplianceDocuments().stream()
                .map(d -> new VariantRequest.ComplianceDocumentDto(d.getCategory(), d.getName(),
                        d.getReference(), d.getAuthority(), d.getApplicableTo(), d.getDate(),
                        d.getExpiryDate(), d.getStatus(), d.getFileName(), d.getFileId(), d.getFileUrl()))
                .toList();

        VariantRequest.VariantDetails details = new VariantRequest.VariantDetails(
                variant.getName(), variant.getVariantType(), variant.getGrade(), variant.getAssayPurity(),
                variant.getPackSize(), variant.getPackagingType(), variant.getParticleSize(),
                variant.getSkuCode(), variant.getBatchPrefix(), variant.getStatus(),
                variant.getImages(), variant.getDetailsExtra());

        VariantRequest.CommercialPricingDto commercial = new VariantRequest.CommercialPricingDto(
                new VariantRequest.PricingQuantity(
                        new VariantRequest.BasePricing(pricing.getBaseCost(), pricing.getUnit(),
                                pricing.getMoq(), pricing.getLeadTime()),
                        tiers,
                        new VariantRequest.CommercialCharges(pricing.getFreightCharges(),
                                pricing.getInsuranceCharges(), pricing.getHandlingCharges(),
                                pricing.getOtherCharges())),
                new VariantRequest.CommercialTradeTerms(pricing.getCurrency(), pricing.getPaymentTerms(),
                        pricing.getIncoterms(), pricing.getPriceValidityDays(), pricing.getGstTaxes(),
                        pricing.getExportAvailable(), pricing.getMinOrderValue(),
                        pricing.getPartialShipmentAllowed(), pricing.getReturnPolicy(),
                        pricing.getWarrantyPeriod()),
                new VariantRequest.PackagingAndSamples(packaging,
                        new VariantRequest.SampleInformation(pricing.getSampleAvailable(),
                                pricing.getFreePaidSample(), pricing.getSampleCost(),
                                pricing.getSampleTurnaroundDays(), pricing.getMaxSampleQty(),
                                pricing.getSampleShippingBorneBy())),
                new VariantRequest.IncludedDocumentsAndServices(pricing.getIncludedDocuments(),
                        pricing.getIncludedServices()));

        int parameterCount = groups.stream().mapToInt(g -> g.dataOrEmpty().size()).sum();

        return new VariantResponse(
                variant.getId().toString(),
                variant.getPosition(),
                details,
                new VariantRequest.TechnicalSpecifications(groups),
                commercial,
                new VariantRequest.ComplianceCertifications(documents),
                variant.getSearchMarketplace(),
                new VariantResponse.Summary(
                        variant.getName(), variant.getVariantType(), variant.getGrade(),
                        variant.getAssayPurity(), variant.getPackSize(), variant.getPackagingType(),
                        variant.getParticleSize(), variant.getSkuCode(), variant.getBatchPrefix(),
                        variant.getStatus(), variant.getImages(),
                        groups.size(), parameterCount, documents.size()),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    // ---- variant: DTO → entity (partial: only sections present in the request) ----

    public void applyVariantDetails(ProductVariant variant, VariantRequest.VariantDetails details) {
        if (details == null) {
            return;
        }
        variant.setName(details.name());
        variant.setVariantType(details.variantType());
        variant.setGrade(details.grade());
        variant.setAssayPurity(details.assayPurity());
        variant.setPackSize(details.packSize());
        variant.setPackagingType(details.packagingType());
        variant.setParticleSize(details.particleSize());
        variant.setSkuCode(details.skuCode());
        variant.setBatchPrefix(details.batchPrefix());
        if (details.status() != null) {
            variant.setStatus(details.status());
        }
        variant.setImages(persistImageList(details.imagesOrEmpty()));
        variant.setDetailsExtra(new LinkedHashMap<>(details.extraOrEmpty()));
    }

    public void applyTechnicalSpecifications(ProductVariant variant,
                                             VariantRequest.TechnicalSpecifications specs) {
        if (specs == null) {
            return;
        }
        List<VariantSpecificationGroup> groups = new ArrayList<>();
        for (VariantRequest.SpecificationGroupDto dto : specs.dataOrEmpty()) {
            VariantSpecificationGroup group = new VariantSpecificationGroup();
            group.setTitle(dto.title());
            group.setTag(dto.tag());
            group.setCollapsed(Boolean.TRUE.equals(dto.collapsed()));

            List<VariantSpecificationParameter> rows = new ArrayList<>();
            for (VariantRequest.SpecificationParameterDto row : dto.dataOrEmpty()) {
                VariantSpecificationParameter parameter = new VariantSpecificationParameter();
                parameter.setParameter(row.parameterName());
                parameter.setSpecification(row.specification());
                parameter.setUnit(row.unit());
                parameter.setMethod(row.testMethodOrStandard());
                parameter.setSource(row.requirementSource());
                parameter.setAttachment(persistMaybeDataUri(row.attachment(), null, "product-spec"));
                parameter.setPriority(row.priority());
                parameter.setRemarks(row.remarks());
                rows.add(parameter);
            }
            group.replaceParameters(rows);
            groups.add(group);
        }
        variant.replaceSpecificationGroups(groups);
    }

    public void applyCommercialPricing(ProductVariant variant, VariantRequest.CommercialPricingDto dto) {
        if (dto == null) {
            return;
        }
        CommercialPricing pricing = variant.getCommercialPricing();

        VariantRequest.PricingQuantity pq = dto.pricingQuantity();
        if (pq != null) {
            VariantRequest.BasePricing base = pq.basePricing();
            if (base != null) {
                pricing.setBaseCost(base.pricePerUnit());
                pricing.setUnit(base.unit());
                pricing.setMoq(base.moq());
                pricing.setLeadTime(base.leadTime());
            }
            VariantRequest.CommercialCharges charges = pq.commercialCharges();
            if (charges != null) {
                pricing.setFreightCharges(charges.freightCharges());
                pricing.setInsuranceCharges(charges.insuranceCharges());
                pricing.setHandlingCharges(charges.handlingCharges());
                pricing.setOtherCharges(charges.otherCharges());
            }
            List<VariantPriceTier> tiers = new ArrayList<>();
            for (VariantRequest.PriceTierDto row : pq.volumePricingOrEmpty()) {
                VariantPriceTier tier = new VariantPriceTier();
                tier.setQtyBreak(row.quantityRange());
                tier.setTierLabel(row.tierName());
                tier.setPricePerUnit(row.pricePerUnit());
                tier.setDiscountVsBase(row.discountVsBase());
                tier.setLeadTimeDays(row.leadTime());
                tiers.add(tier);
            }
            variant.replacePriceTiers(tiers);
        }

        VariantRequest.CommercialTradeTerms terms = dto.commercialTradeTerms();
        if (terms != null) {
            pricing.setCurrency(terms.currency());
            pricing.setPaymentTerms(terms.paymentTerms());
            pricing.setIncoterms(terms.incoterms());
            pricing.setPriceValidityDays(terms.priceValidityDays());
            pricing.setGstTaxes(terms.gstTaxes());
            pricing.setExportAvailable(terms.exportAvailable());
            pricing.setMinOrderValue(terms.minOrderValue());
            pricing.setPartialShipmentAllowed(terms.partialShipmentAllowed());
            pricing.setReturnPolicy(terms.returnPolicy());
            pricing.setWarrantyPeriod(terms.warrantyPeriod());
        }

        VariantRequest.PackagingAndSamples packaging = dto.packagingAndSamples();
        if (packaging != null) {
            List<VariantPackagingOption> options = new ArrayList<>();
            for (VariantRequest.PackagingOptionDto row : packaging.packagingOrEmpty()) {
                VariantPackagingOption option = new VariantPackagingOption();
                option.setPackagingType(row.packagingType());
                option.setSize(row.size());
                option.setCustomPackaging(row.customPackaging());
                options.add(option);
            }
            variant.replacePackagingOptions(options);

            VariantRequest.SampleInformation sample = packaging.sampleInformation();
            if (sample != null) {
                pricing.setSampleAvailable(sample.sampleAvailable());
                pricing.setFreePaidSample(sample.freePaidSample());
                pricing.setSampleCost(sample.sampleCost());
                pricing.setSampleTurnaroundDays(sample.sampleTurnaroundDays());
                pricing.setMaxSampleQty(sample.maxSampleQty());
                pricing.setSampleShippingBorneBy(sample.sampleShippingBorneBy());
            }
        }

        VariantRequest.IncludedDocumentsAndServices included = dto.includedDocumentsAndServices();
        if (included != null) {
            pricing.setIncludedDocuments(new ArrayList<>(included.documentsOrEmpty()));
            pricing.setIncludedServices(new ArrayList<>(included.servicesOrEmpty()));
        }
    }

    public void applyComplianceDocuments(ProductVariant variant,
                                         VariantRequest.ComplianceCertifications compliance) {
        if (compliance == null) {
            return;
        }
        List<VariantComplianceDocument> documents = new ArrayList<>();
        for (VariantRequest.ComplianceDocumentDto dto : compliance.dataOrEmpty()) {
            VariantComplianceDocument document = new VariantComplianceDocument();
            document.setCategory(dto.category());
            document.setName(dto.name());
            document.setReference(dto.reference());
            document.setAuthority(dto.authority());
            document.setApplicableTo(dto.applicableTo());
            document.setDate(dto.date());
            document.setExpiryDate(dto.expiryDate());
            document.setStatus(dto.status());
            document.setFileName(dto.fileName());
            document.setFileId(dto.fileId());
            document.setFileUrl(dto.fileUrl());
            persistComplianceFile(document);
            documents.add(document);
        }
        variant.replaceComplianceDocuments(documents);
    }

    public void applySearchMarketplace(ProductVariant variant, Map<String, Object> searchMarketplace) {
        if (searchMarketplace == null) {
            return;
        }
        variant.setSearchMarketplace(new LinkedHashMap<>(searchMarketplace));
    }

    /** Applies every section present in the request; absent sections are untouched. */
    public void applyVariant(ProductVariant variant, VariantRequest request) {
        applyVariantDetails(variant, request.variantDetails());
        applyTechnicalSpecifications(variant, request.technicalSpecifications());
        applyCommercialPricing(variant, request.commercialPricing());
        applyComplianceDocuments(variant, request.complianceCertifications());
        applySearchMarketplace(variant, request.searchMarketplace());
    }

    private CommercialMaster resolveCommercial(VendorProduct product) {
        if (product.getCommercialMasterId() == null) {
            return null;
        }
        return commercialMasters.findById(product.getCommercialMasterId()).orElse(null);
    }

    private List<String> persistImageList(List<String> images) {
        List<String> out = new ArrayList<>();
        for (String image : images) {
            out.add(persistMaybeDataUri(image, null, "product-image"));
        }
        return out;
    }

    private void persistComplianceFile(VariantComplianceDocument document) {
        DataUriPersister.StoredRef stored = dataUriPersister.persist(
                document.getFileUrl(), document.getFileName(), "product-document");
        if (stored == null) {
            return;
        }
        document.setFileId(stored.id());
        document.setFileUrl(stored.url());
        if (stored.fileName() != null) {
            document.setFileName(stored.fileName());
        }
    }

    private String persistMaybeDataUri(String value, String fileName, String module) {
        DataUriPersister.StoredRef stored = dataUriPersister.persist(value, fileName, module);
        if (stored == null) {
            return value;
        }
        return stored.url();
    }
}
