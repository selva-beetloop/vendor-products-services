package com.beetloop.vendorproducts.repository;

import com.beetloop.vendorproducts.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderByPositionAsc(UUID productId);
}
