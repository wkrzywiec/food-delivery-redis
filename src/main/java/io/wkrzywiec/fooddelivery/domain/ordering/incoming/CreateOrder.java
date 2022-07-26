package io.wkrzywiec.fooddelivery.domain.ordering.incoming;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrder(
        String customerId,
        String restaurantId,
        List<Item> items,
        String address,
        BigDecimal deliveryCharge) {
}
