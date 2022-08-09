package io.wkrzywiec.fooddelivery.domain.delivery.outgoing;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
