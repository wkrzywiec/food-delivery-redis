package io.wkrzywiec.fooddelivery.domain.ordering.incoming;

import java.math.BigDecimal;

public record AddTip(String orderId, BigDecimal tip) {
}
