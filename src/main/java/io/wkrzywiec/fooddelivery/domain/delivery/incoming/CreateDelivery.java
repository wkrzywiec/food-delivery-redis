package io.wkrzywiec.fooddelivery.domain.delivery.incoming;


import java.util.List;

public record CreateDelivery(String customerId, String restaurantId, List<Item> items, String address) {
}
