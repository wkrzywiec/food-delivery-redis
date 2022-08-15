package io.wkrzywiec.fooddelivery.delivery;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

@Component
class InMemoryDeliveryRepository implements DeliveryRepository {

    final Map<String, Delivery> database = new ConcurrentHashMap<>();

    @Override
    public Delivery save(Delivery newDelivery) {
        var id = newDelivery.getOrderId();
        if (isNull(id)) {
            id = UUID.randomUUID().toString();

            try {
                FieldUtils.writeField(newDelivery, "id", id, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Class 'Order' does not have 'orderId' field");
            }
        }
        database.put(id, newDelivery);
        return newDelivery;
    }

     @Override
     public Optional<Delivery> findByOrderId(String orderId) {
         return database.values().stream()
                 .filter(d -> Objects.equals(d.getOrderId(), orderId))
                 .findAny();
     }
 }
