package io.wkrzywiec.fooddelivery.delivery.outgoing;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
