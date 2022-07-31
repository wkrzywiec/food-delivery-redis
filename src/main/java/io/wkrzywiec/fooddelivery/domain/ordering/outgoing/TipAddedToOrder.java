package io.wkrzywiec.fooddelivery.domain.ordering.outgoing;

import java.math.BigDecimal;

public record TipAddedToOrder(String orderId, BigDecimal tip, BigDecimal total) {
}
