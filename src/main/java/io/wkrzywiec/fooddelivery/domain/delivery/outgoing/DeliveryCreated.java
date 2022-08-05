package io.wkrzywiec.fooddelivery.domain.delivery.outgoing;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryCreated (String id, String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) {
}
