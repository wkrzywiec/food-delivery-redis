package io.wkrzywiec.fooddelivery.commons.infra;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
@Profile("redis")
public class RedisConfig {

    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
                        DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
                )
                .modules(
                        new JavaTimeModule()
                )
                .build();
    }
}
