package io.wkrzywiec.fooddelivery.commons.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record AddTip(String orderId, BigDecimal tip) implements DomainMessageBody {
}
