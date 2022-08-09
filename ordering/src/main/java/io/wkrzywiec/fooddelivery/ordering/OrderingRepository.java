package io.wkrzywiec.fooddelivery.ordering;

import java.util.Optional;

interface OrderingRepository {

    public Order save(Order newOrder);
    public Optional<Order> findById(String id);
}
