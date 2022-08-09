package io.wkrzywiec.fooddelivery.domain.delivery.incoming;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreated(String id, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) {
}
