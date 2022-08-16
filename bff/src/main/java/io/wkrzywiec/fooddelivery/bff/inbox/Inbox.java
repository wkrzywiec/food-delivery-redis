package io.wkrzywiec.fooddelivery.bff.inbox;

public interface Inbox {

    void storeMessage(String channel, Object message);
}
