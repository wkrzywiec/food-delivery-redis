package io.wkrzywiec.fooddelivery.ordering.incoming;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
