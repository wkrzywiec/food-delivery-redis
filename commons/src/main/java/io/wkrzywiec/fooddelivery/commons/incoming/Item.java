package io.wkrzywiec.fooddelivery.commons.incoming;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
