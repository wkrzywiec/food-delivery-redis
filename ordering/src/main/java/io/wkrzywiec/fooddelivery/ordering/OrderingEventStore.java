package io.wkrzywiec.fooddelivery.ordering;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.List;

interface OrderingEventStore {

    void store(Message event);

    List<DomainMessageBody> getEventsForOrder(String orderId);
}
