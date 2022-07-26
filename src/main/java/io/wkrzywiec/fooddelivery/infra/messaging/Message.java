package io.wkrzywiec.fooddelivery.infra.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Message(Header header, String body) {

    public static Message from(Header header, Object body) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var jsonBody = mapper.writeValueAsString(body);
        return new Message(header, jsonBody);
    }
}
