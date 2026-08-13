package com.beetloop.catalog.shared.model;

import java.math.BigDecimal;

/** Unit-paired numeric: Capacity [L;Kg;Tons/hr], Power [kW;HP], MOQ [pcs;kg;rolls]. */
public record Measure(BigDecimal value, String unit) {
}
