package io.wkrzywiec.fooddelivery.delivery;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

class InMemoryDeliveryRepository implements DeliveryRepository {

    final Map<String, Delivery> database = new ConcurrentHashMap<>();

    @Override
    public Delivery save(Delivery newDelivery) {
        var id = newDelivery.getId();
        if (isNull(id)) {
            id = UUID.randomUUID().toString();

            try {
                FieldUtils.writeField(newDelivery, "id", id, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Class 'Order' does not have 'id' field");
            }
        }
        database.put(id, newDelivery);
        return newDelivery;
    }

     @Override
     public Optional<Delivery> findById(String id) {
         return ofNullable(database.get(id));
     }

     @Override
     public Optional<Delivery> findByOrderId(String orderId) {
         return database.values().stream()
                 .filter(d -> Objects.equals(d.getOrderId(), orderId))
                 .findAny();
     }
 }
