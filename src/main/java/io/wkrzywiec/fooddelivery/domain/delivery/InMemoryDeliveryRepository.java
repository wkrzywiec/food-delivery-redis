package io.wkrzywiec.fooddelivery.domain.delivery;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

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
}
