package io.wkrzywiec.fooddelivery.domain.ordering.incoming;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
