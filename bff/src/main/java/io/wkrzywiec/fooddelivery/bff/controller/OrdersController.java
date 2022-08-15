package io.wkrzywiec.fooddelivery.bff.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class OrdersController {

    @PostMapping("/orders/")
    ResponseEntity<Void> createAnOrder(@RequestBody CreateOrderDTO createOrder) {
        log.info("Received request to create an order: {}", createOrder);


        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/orders/{orderId}")
    ResponseEntity<Void> updateAnOrder(String orderId, @RequestBody UpdateOrderDTO updateOrder) {
        log.info("Received request to update an '{}' order, update: {}", orderId, updateOrder);


        return ResponseEntity.accepted().build();
    }

    @PostMapping("/orders/{orderId}/tip")
    ResponseEntity<Void> addTip(String orderId, @RequestBody AddTipDTO updateOrder) {
        log.info("Received request to add tip to '{}' an order, value: {}", orderId, updateOrder);


        return ResponseEntity.accepted().build();
    }
}
