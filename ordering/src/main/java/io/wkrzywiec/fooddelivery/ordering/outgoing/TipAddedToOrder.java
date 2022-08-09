package io.wkrzywiec.fooddelivery.ordering.outgoing;

import java.math.BigDecimal;

public record TipAddedToOrder(String orderId, BigDecimal tip, BigDecimal total) {
}
