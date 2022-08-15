package io.wkrzywiec.fooddelivery.commons.infra.messaging.redis;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;

public interface RedisStreamListener extends StreamListener<String, MapRecord<String, String, String>> {


    String streamName();
    String group();
    String consumer();

}
