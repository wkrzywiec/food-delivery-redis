package io.wkrzywiec.fooddelivery.delivery.incoming;


import java.util.List;

public record CreateDelivery(String customerId, String restaurantId, List<Item> items, String address) {
}
