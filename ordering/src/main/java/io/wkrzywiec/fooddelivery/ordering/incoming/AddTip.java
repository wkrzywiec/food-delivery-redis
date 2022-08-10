package io.wkrzywiec.fooddelivery.ordering.incoming;

import java.math.BigDecimal;

public record AddTip(String orderId, BigDecimal tip) {
}
