package io.wkrzywiec.fooddelivery.delivery.infra.messaging;

import java.util.List;

public interface MessagePublisher {
    void send(Message message);

    default void send(List<Message> messages) {
        messages.forEach(this::send);
    }
}
