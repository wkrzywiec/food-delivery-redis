package io.wkrzywiec.fooddelivery.delivery.outgoing;

public record DeliveryCanceled(String deliveryId, String orderId, String reason) {
}
