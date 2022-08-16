package io.wkrzywiec.fooddelivery.commons.incoming;

import java.math.BigDecimal;

public record AddTip(String orderId, BigDecimal tip) {
}
