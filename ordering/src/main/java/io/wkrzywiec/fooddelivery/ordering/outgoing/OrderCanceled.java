package io.wkrzywiec.fooddelivery.ordering.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCanceled(String orderId, String reason) implements DomainMessageBody {
}
