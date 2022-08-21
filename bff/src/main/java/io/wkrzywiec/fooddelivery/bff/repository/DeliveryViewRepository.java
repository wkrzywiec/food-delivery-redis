package io.wkrzywiec.fooddelivery.bff.repository;

import io.wkrzywiec.fooddelivery.bff.view.DeliveryView;

import java.util.List;
import java.util.Optional;

public interface DeliveryViewRepository {
    List<DeliveryView> getAllDeliveryViews();
    Optional<DeliveryView> getDeliveryViewById(String orderId);

}
