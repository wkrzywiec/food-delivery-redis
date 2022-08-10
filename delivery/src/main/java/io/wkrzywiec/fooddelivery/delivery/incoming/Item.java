package io.wkrzywiec.fooddelivery.delivery.incoming;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
