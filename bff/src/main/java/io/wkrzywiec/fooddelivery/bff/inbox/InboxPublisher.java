package io.wkrzywiec.fooddelivery.bff.inbox;

public interface InboxPublisher {

    void storeMessage(String channel, Object message);
}
