package io.wkrzywiec.fooddelivery.bff.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import io.wkrzywiec.fooddelivery.bff.controller.model.ChangeDeliveryManDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.ResponseDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.UpdateDeliveryDTO;
import io.wkrzywiec.fooddelivery.bff.repository.DeliveryViewRepository;
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

    private final DeliveryViewRepository repository;
    private final InboxPublisher inboxPublisher;
    private static final String DELIVERY_INBOX = "delivery-inbox";

    @GetMapping("/deliveries")
    ResponseEntity<List<DeliveryView>> getAllDeliveries() {
        var deliveryViews = repository.getAllDeliveryViews();
        return ResponseEntity.ok(deliveryViews);
    }

    @GetMapping("/deliveries/{orderId}")
    ResponseEntity<DeliveryView> getDeliveryById(@PathVariable String orderId) {
        var deliveryView = repository.getDeliveryViewById(orderId);

        if (deliveryView.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deliveryView.get());
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
