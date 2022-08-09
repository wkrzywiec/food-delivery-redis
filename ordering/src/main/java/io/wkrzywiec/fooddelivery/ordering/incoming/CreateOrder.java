package io.wkrzywiec.fooddelivery.ordering.incoming;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrder(
        String id,
        String customerId,
        String restaurantId,
        List<Item> items,
        String address,
        BigDecimal deliveryCharge) {
}
