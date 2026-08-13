package com.beetloop.catalog.shared.model;

import java.math.BigDecimal;

/** The `Dimensions (L x W x H)` triple with a shared unit. */
public record Dimensions(BigDecimal length, BigDecimal width, BigDecimal height, String unit) {
}
