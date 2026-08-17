package com.beetloop.vendorproducts.catalogue;

import com.beetloop.vendorproducts.catalogue.domain.CatalogueKind;
import com.beetloop.vendorproducts.catalogue.domain.CatalogueStatus;
import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.catalogue.domain.ScientificMaster;
import com.beetloop.vendorproducts.catalogue.repository.CommercialMasterRepository;
import com.beetloop.vendorproducts.catalogue.repository.ScientificMasterRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Seeds approved T1/T2 rows so Flow A search is non-empty. */
@Component
public class CatalogueSeed implements ApplicationRunner {

    private final ScientificMasterRepository scientificMasters;
    private final CommercialMasterRepository commercialMasters;

    public CatalogueSeed(ScientificMasterRepository scientificMasters,
                         CommercialMasterRepository commercialMasters) {
        this.scientificMasters = scientificMasters;
        this.commercialMasters = commercialMasters;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ScientificMaster curcumin = t1("SCC-CUR-001", CatalogueKind.PRODUCT, "raw-materials",
                "Curcumin", "458-37-7", "C21H20O6");
        t2("CM-CUR95-001", curcumin, "Curcumin 95% Extract", "≥ 95%", "Food Grade",
                "Fine Powder", "India", "Orange", "Rhizome", Map.of(
                        "extractName", "Curcumin 95% Extract",
                        "botanicalName", "Curcuma longa",
                        "identityType", "botanical-extract",
                        "plantPartUsed", "Rhizome",
                        "extractType", "Standardized Extract",
                        "casNumber", "458-37-7"));

        ScientificMaster wheat = t1("SCC-WHE-001", CatalogueKind.PRODUCT, "raw-materials",
                "Wheat", "", "C6H10O5");
        t2("CM-WHEFG-001", wheat, "Wheat Food Grade", "12.5% moisture", "Food Grade",
                "Grain", "India", "Cream", "Crop", Map.of("commodityName", "Wheat", "identityType", "raw-commodity"));

        ScientificMaster blender = t1("SCC-RBN-001", CatalogueKind.PRODUCT, "processing-machinery",
                "Ribbon Blender", "", "");
        t2("CM-RB500-001", blender, "Ribbon Blender RB-500", "", "Industrial",
                "Machine", "India", "Steel", "OEM", Map.of("machineName", "Ribbon Blender RB-500"));

        ScientificMaster drink = t1("SCC-HYD-001", CatalogueKind.PRODUCT, "finished-goods",
                "Electrolyte Beverage", "", "");
        t2("CM-HYDRA-001", drink, "HydraFit Electrolyte Drink", "", "Commercial",
                "RTD", "USA", "", "Brand", Map.of("commercialProductName", "HydraFit Electrolyte Drink"));

        ScientificMaster pouch = t1("SCC-PET-001", CatalogueKind.PRODUCT, "packaging-materials",
                "PET Packaging", "", "");
        t2("CM-PET500-001", pouch, "PET Bottle 500ml", "", "Food Grade",
                "Bottle", "India", "Clear", "Resin", Map.of(
                        "productName", "PET Bottle 500ml",
                        "packagingSubCategory", "Bottles"));

        ScientificMaster vfs = t1("SCC-VFS-001", CatalogueKind.PRODUCT, "packaging-machinery",
                "Vertical FFS", "", "");
        t2("CM-VFS200-001", vfs, "Vertical FFS VFS-200", "", "Automatic",
                "Machine", "India", "", "OEM", Map.of("machineName", "Vertical FFS VFS-200"));

        ScientificMaster hplc = t1("SCC-HPLC-001", CatalogueKind.SERVICE, "lab-testing",
                "HPLC Assay", "", "");
        t2("CM-HPLC-001", hplc, "HPLC Assay", "", "Accredited",
                "Lab", "India", "", "Method", Map.of("name", "HPLC Assay"));
    }

    private ScientificMaster t1(String code, CatalogueKind kind, String category,
                                String name, String cas, String formula) {
        return scientificMasters.findByCode(code).orElseGet(() -> {
            ScientificMaster s = new ScientificMaster();
            s.setCode(code);
            s.setKind(kind);
            s.setCategory(category);
            s.setName(name);
            s.setCasNumber(cas);
            s.setFormula(formula);
            s.setStatus(CatalogueStatus.LIVE);
            s.setPayload(new LinkedHashMap<>());
            return scientificMasters.save(s);
        });
    }

    private void t2(String code, ScientificMaster t1, String name, String assay, String grade,
                    String form, String origin, String colour, String source, Map<String, Object> extra) {
        if (commercialMasters.existsByCode(code)) {
            return;
        }
        CommercialMaster c = new CommercialMaster();
        c.setCode(code);
        c.setScientificMaster(t1);
        c.setKind(t1.getKind());
        c.setCategory(t1.getCategory());
        c.setName(name);
        c.setAssay(assay);
        c.setGrade(grade);
        c.setForm(form);
        c.setOrigin(origin);
        c.setColour(colour);
        c.setSource(source);
        c.setStatus(CatalogueStatus.LIVE);
        Map<String, Object> baseline = new LinkedHashMap<>(extra);
        baseline.put("casNumber", t1.getCasNumber());
        baseline.put("casNo", t1.getCasNumber());
        baseline.put("botanicalName", extra.getOrDefault("botanicalName", t1.getName()));
        baseline.put("assay", assay);
        baseline.put("grade", grade);
        baseline.put("form", form);
        baseline.put("origin", origin);
        baseline.put("colour", colour);
        baseline.put("source", source);
        c.setBaseline(baseline);
        c.refreshGradeKey();
        commercialMasters.save(c);
    }
}
