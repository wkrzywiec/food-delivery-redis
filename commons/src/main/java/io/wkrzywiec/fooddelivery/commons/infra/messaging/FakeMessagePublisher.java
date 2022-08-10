package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FakeMessagePublisher implements MessagePublisher {

    ConcurrentHashMap<String, List<Message>> messages = new ConcurrentHashMap<>();

    @Override
    public void send(Message message) {
      log.info("Publishing '{}' message on channel: '{}', body: '{}'", message.header().type(), message.header().channel(), message.body());
      var channel = message.header().channel();

      var messagesInChannel = messages.getOrDefault(channel, new ArrayList<>());
      messagesInChannel.add(message);
      messages.put(message.header().channel(), messagesInChannel);

      log.info("'{}' message was published on channel: '{}', full message: '{}'", message.header().type(), message.header().channel(), message);
    }
}
