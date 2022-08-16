package io.wkrzywiec.fooddelivery.bff.controller;

import io.wkrzywiec.fooddelivery.bff.inbox.Inbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
public class OrdersController {

    private final Inbox inbox;
    private static final String ORDERING_INBOX = "ordering-inbox";

    @PostMapping("/orders/")
    ResponseEntity<ResponseDTO> createAnOrder(@RequestBody CreateOrderDTO createOrder) {
        log.info("Received request to create an order: {}", createOrder);
        if (createOrder.getId() == null) {
            var id = UUID.randomUUID().toString();
            createOrder.setId(id);
            log.info("Generated {} id for a new order", id);
        }

        inbox.storeMessage(ORDERING_INBOX, createOrder);

        return ResponseEntity.accepted().body(new ResponseDTO(createOrder.getId()));
    }

    @PatchMapping("/orders/{orderId}")
    ResponseEntity<ResponseDTO> updateAnOrder(String orderId, @RequestBody UpdateOrderDTO updateOrder) {
        log.info("Received request to update an '{}' order, update: {}", orderId, updateOrder);
        inbox.storeMessage(ORDERING_INBOX, updateOrder);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }

    @PostMapping("/orders/{orderId}/tip")
    ResponseEntity<ResponseDTO> addTip(String orderId, @RequestBody AddTipDTO addTip) {
        log.info("Received request to add tip to '{}' an order, value: {}", orderId, addTip);
        inbox.storeMessage(ORDERING_INBOX, addTip);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }
}
