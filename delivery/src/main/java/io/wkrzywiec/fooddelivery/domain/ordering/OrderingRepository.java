package io.wkrzywiec.fooddelivery.domain.ordering;

import java.util.Optional;

interface OrderingRepository {

    public Order save(Order newOrder);
    public Optional<Order> findById(String id);
}
