package io.wkrzywiec.fooddelivery.delivery.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodInPreparation(String orderId) implements DomainMessageBody {
}
