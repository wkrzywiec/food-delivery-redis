package io.wkrzywiec.fooddelivery.domain.delivery.outgoing;

public record DeliveryCanceled(String deliveryId, String orderId, String reason) {
}
