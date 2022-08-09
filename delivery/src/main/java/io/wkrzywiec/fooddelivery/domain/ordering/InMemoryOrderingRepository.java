package io.wkrzywiec.fooddelivery.domain.ordering;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

class InMemoryOrderingRepository implements OrderingRepository {

    final Map<String, Order> database = new ConcurrentHashMap<>();

    @Override
    public Order save(Order newOrder) {
        var id = newOrder.getId();
        if (isNull(id)) {
            id = UUID.randomUUID().toString();

            try {
                FieldUtils.writeField(newOrder, "id", id, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Class 'Order' does not have 'id' field");
            }
        }
        database.put(id, newOrder);
        return newOrder;
    }

    @Override
    public Optional<Order> findById(String id) {
        return ofNullable(database.get(id));
    }
}
