package io.wkrzywiec.fooddelivery.commons.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record CancelOrder(String orderId, String reason) implements DomainMessageBody {
}
