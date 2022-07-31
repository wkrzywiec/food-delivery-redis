package io.wkrzywiec.fooddelivery.domain.delivery;

import io.wkrzywiec.fooddelivery.domain.delivery.incoming.*;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCanceled;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated;

public class DeliveryFacade {

    public void handle(OrderCreated orderCreated) {
    }

    public void handle(OrderCanceled cancelDelivery) {

    }

    public void handle(PrepareFood cancelDelivery) {

    }

    public void handle(AssignedDeliveryMan assignedDeliveryMan) {

    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {

    }

    public void handle(FoodReady foodReady) {

    }

    public void handle(PickUpFood pickUpFood) {

    }

    public void handle(DeliverFood deliverFood) {

    }
}
