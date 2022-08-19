package io.wkrzywiec.fooddelivery.commons.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record AssignDeliveryMan(String orderId, String deliveryManId) implements DomainMessageBody {
}
