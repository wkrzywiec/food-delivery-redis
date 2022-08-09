package io.wkrzywiec.fooddelivery.delivery;

import java.util.Optional;

interface DeliveryRepository {

    Delivery save(Delivery delivery);
    Optional<Delivery> findById(String id);
    Optional<Delivery> findByOrderId(String orderId);
}
