package io.wkrzywiec.fooddelivery.bff.view.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManAssigned(String orderId, String deliveryManId) implements DomainMessageBody {
}
