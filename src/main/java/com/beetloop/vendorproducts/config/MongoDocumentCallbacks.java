package com.beetloop.vendorproducts.config;

import com.beetloop.vendorproducts.catalogue.domain.CommercialMaster;
import com.beetloop.vendorproducts.domain.ProductVariant;
import com.beetloop.vendorproducts.domain.VariantSpecificationGroup;
import com.beetloop.vendorproducts.domain.VendorProduct;
import com.beetloop.vendorproducts.pm.domain.PmOrder;
import com.beetloop.vendorproducts.pm.domain.PmProject;
import com.beetloop.vendorproducts.pm.domain.PmStage;
import com.beetloop.vendorproducts.services.domain.ServiceDocument;
import com.beetloop.vendorproducts.services.domain.VendorService;
import com.beetloop.vendorproducts.services.domain.VendorServiceBatch;
import org.bson.Document;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Assigns nested UUIDs/timestamps on write and restores parent back-references after load.
 */
@Component
public class MongoDocumentCallbacks implements BeforeConvertCallback<Object>, AfterConvertCallback<Object> {

    @Override
    public Object onBeforeConvert(Object entity, String collection) {
        walkAssign(entity, new IdentityHashMap<>());
        return entity;
    }

    @Override
    public Object onAfterConvert(Object entity, Document document, String collection) {
        relink(entity);
        return entity;
    }

    private static void walkAssign(Object entity, IdentityHashMap<Object, Boolean> seen) {
        if (entity == null || seen.put(entity, Boolean.TRUE) != null) {
            return;
        }
        if (entity instanceof Collection<?> collection) {
            collection.forEach(item -> walkAssign(item, seen));
            return;
        }
        if (entity instanceof Map<?, ?> map) {
            map.values().forEach(item -> walkAssign(item, seen));
            return;
        }
        Class<?> type = entity.getClass();
        Package pkg = type.getPackage();
        if (pkg == null || !pkg.getName().startsWith("com.beetloop.vendorproducts")) {
            return;
        }
        ensureUuid(entity);
        touchTimestamps(entity);
        if (entity instanceof CommercialMaster master) {
            master.refreshGradeKey();
            if (master.getScientificMaster() != null) {
                master.setScientificMaster(master.getScientificMaster());
            }
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.getAnnotation(Transient.class) != null) {
                continue;
            }
            field.setAccessible(true);
            try {
                walkAssign(field.get(entity), seen);
            } catch (IllegalAccessException ignored) {
                // skip unreadable fields
            }
        }
    }

    private static void ensureUuid(Object entity) {
        Field id = field(entity.getClass(), "id");
        if (id == null || id.getType() != UUID.class) {
            return;
        }
        id.setAccessible(true);
        try {
            if (id.get(entity) == null) {
                id.set(entity, UUID.randomUUID());
            }
        } catch (IllegalAccessException ignored) {
            // keep existing id
        }
    }

    private static void touchTimestamps(Object entity) {
        Instant now = Instant.now();
        Field createdAt = field(entity.getClass(), "createdAt");
        Field updatedAt = field(entity.getClass(), "updatedAt");
        try {
            if (createdAt != null && createdAt.getType() == Instant.class) {
                createdAt.setAccessible(true);
                if (createdAt.get(entity) == null) {
                    createdAt.set(entity, now);
                }
            }
            if (updatedAt != null && updatedAt.getType() == Instant.class) {
                updatedAt.setAccessible(true);
                updatedAt.set(entity, now);
            }
        } catch (IllegalAccessException ignored) {
            // keep existing timestamps
        }
    }

    private static Field field(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    static void relink(Object entity) {
        if (entity instanceof VendorProduct product) {
            relinkProduct(product);
        } else if (entity instanceof VendorServiceBatch batch) {
            relinkBatch(batch);
        } else if (entity instanceof PmProject project) {
            relinkProject(project);
        }
    }

    private static void relinkProduct(VendorProduct product) {
        if (product.getVariants() == null) {
            return;
        }
        for (ProductVariant variant : product.getVariants()) {
            variant.setProduct(product);
            if (variant.getSpecificationGroups() != null) {
                for (VariantSpecificationGroup group : variant.getSpecificationGroups()) {
                    group.setVariant(variant);
                    if (group.getParameters() != null) {
                        group.getParameters().forEach(parameter -> parameter.setGroup(group));
                    }
                }
            }
            if (variant.getPriceTiers() != null) {
                variant.getPriceTiers().forEach(tier -> tier.setVariant(variant));
            }
            if (variant.getPackagingOptions() != null) {
                variant.getPackagingOptions().forEach(option -> option.setVariant(variant));
            }
            if (variant.getComplianceDocuments() != null) {
                variant.getComplianceDocuments().forEach(document -> document.setVariant(variant));
            }
        }
    }

    private static void relinkBatch(VendorServiceBatch batch) {
        if (batch.getItems() == null) {
            return;
        }
        for (VendorService item : batch.getItems()) {
            item.setBatch(batch);
            if (item.getDocuments() != null) {
                for (ServiceDocument document : item.getDocuments()) {
                    document.setService(item);
                }
            }
        }
    }

    private static void relinkProject(PmProject project) {
        if (project.getLineItems() != null) {
            project.getLineItems().forEach(item -> item.setProject(project));
        }
        if (project.getOrders() == null) {
            return;
        }
        for (PmOrder order : project.getOrders()) {
            order.setProject(project);
            if (order.getStages() == null) {
                continue;
            }
            for (PmStage stage : order.getStages()) {
                stage.setOrder(order);
                if (stage.getTasks() != null) {
                    stage.getTasks().forEach(task -> task.setStage(stage));
                }
                if (stage.getChecklist() != null) {
                    stage.getChecklist().forEach(item -> item.setStage(stage));
                }
            }
        }
    }
}
