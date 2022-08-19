package io.wkrzywiec.fooddelivery.commons.infra.repository;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.List;

public interface EventStore {

    void store(Message event);
    List<DomainMessageBody> getEventsForOrder(String orderId);
}
