package io.wkrzywiec.fooddelivery.bff.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import io.wkrzywiec.fooddelivery.bff.controller.model.ChangeDeliveryManDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.ResponseDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.UpdateDeliveryDTO;
import io.wkrzywiec.fooddelivery.bff.view.DeliveryView;
import io.wkrzywiec.fooddelivery.bff.view.outgoing.DeliveryCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Optional.ofNullable;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final InboxPublisher inboxPublisher;
    private static final String DELIVERY_INBOX = "delivery-inbox";

    @GetMapping("/deliveries")
    ResponseEntity<List<DeliveryView>> getAllDeliveries() {
        Set<Object> keys = redisTemplate.opsForHash().keys("delivery-view");

        Map<Object, Object> entries = redisTemplate.opsForHash().entries("delivery-view");

        List<DeliveryView> deliveryViews = entries.values().stream()
                .map(this::mapJsonStringToDeliveryView)
                .toList();

        return ResponseEntity.ok(deliveryViews);
    }

    @GetMapping("/deliveries/{orderId}")
    ResponseEntity<DeliveryView> getDeliveryById(@PathVariable String orderId) {
        DeliveryView deliveryView = getDeliveryViewById(orderId);

        if (deliveryView == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deliveryView);
    }

    private DeliveryView getDeliveryViewById(String orderId) {
        var deliveryViewOptional = ofNullable(redisTemplate.opsForHash().get("delivery-view", orderId));
        return deliveryViewOptional.map(this::mapJsonStringToDeliveryView)
                .orElse(null);
    }

    private DeliveryView mapJsonStringToDeliveryView(Object deliveryView) {
        try {
            return objectMapper.readValue((String) deliveryView, DeliveryView.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PatchMapping("/deliveries/{orderId}")
    ResponseEntity<ResponseDTO> updateADelivery(@PathVariable String orderId, @RequestBody UpdateDeliveryDTO updateDelivery) {
        log.info("Received request to update a delivery for an '{}' order, update: {}", orderId, updateDelivery);
        updateDelivery.setOrderId(orderId);
        inboxPublisher.storeMessage(DELIVERY_INBOX + ":update", updateDelivery);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }

    @PostMapping("/deliveries/{orderId}/delivery-man")
    ResponseEntity<ResponseDTO> deliveryMan(@PathVariable String orderId, @RequestBody ChangeDeliveryManDTO changeDeliveryMan) {
        log.info("Received request to assign '{}' delivery man to an '{}' order", changeDeliveryMan.getDeliveryManId(), orderId);
        changeDeliveryMan.setOrderId(orderId);
        inboxPublisher.storeMessage(DELIVERY_INBOX + ":delivery-man", changeDeliveryMan);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }
}
