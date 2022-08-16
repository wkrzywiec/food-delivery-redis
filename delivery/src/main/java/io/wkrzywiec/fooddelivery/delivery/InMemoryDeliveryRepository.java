package io.wkrzywiec.fooddelivery.delivery;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
class InMemoryDeliveryRepository implements DeliveryRepository {

    final Map<String, Delivery> database = new ConcurrentHashMap<>();

    @Override
    public Delivery save(Delivery newDelivery) {
        var id = newDelivery.getOrderId();
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
