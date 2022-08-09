package io.wkrzywiec.fooddelivery.delivery.infra.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Message(Header header, String body) {

    public static Message from(Header header, Object body) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var jsonBody = mapper.writeValueAsString(body);
        //todo think how to solve better assembling a message so the type is in the header
        return new Message(header, jsonBody);
    }
}
