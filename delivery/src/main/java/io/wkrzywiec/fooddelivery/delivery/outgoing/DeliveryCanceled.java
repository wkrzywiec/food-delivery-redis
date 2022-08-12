package io.wkrzywiec.fooddelivery.delivery.outgoing;

public record DeliveryCanceled(String orderId, String reason) {
}
