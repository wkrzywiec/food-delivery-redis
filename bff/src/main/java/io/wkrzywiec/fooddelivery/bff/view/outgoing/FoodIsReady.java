package io.wkrzywiec.fooddelivery.bff.view.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodIsReady(String orderId) implements DomainMessageBody {
}
