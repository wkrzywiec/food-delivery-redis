package io.wkrzywiec.fooddelivery.domain.ordering;

interface OrderingRepository {

    public Order save(Order newOrder);
}
