package io.wkrzywiec.fooddelivery.commons.messaging

import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import spock.lang.Specification

import java.time.Instant

class FakeMessagePublisherSpec extends Specification {

    FakeMessagePublisher publisher

    def setup() {
        publisher = new FakeMessagePublisher()
    }

    def "Send message"() {
        when:
        publisher.send(
                new Message(
                        new Header("messageId", "channel", "messageType", "itemId", Instant.now()),
                        "{}"))

        then:
        publisher.messages.get("channel").size() == 1
    }
}
