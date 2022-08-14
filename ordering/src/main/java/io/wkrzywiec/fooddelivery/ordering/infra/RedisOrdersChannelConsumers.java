package io.wkrzywiec.fooddelivery.ordering.infra;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("redis")
public class RedisOrdersChannelConsumers implements RedisStreamListener {

    @Override
    public String streamName() {
        return "orders";
    }

    @Override
    public String group() {
        return "ordering";
    }

    @Override
    public String consumer() {
        //TODO randomize ?
        return "1";
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("Got message: {}", message);
    }
}
