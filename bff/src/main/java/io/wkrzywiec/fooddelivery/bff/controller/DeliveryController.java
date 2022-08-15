package io.wkrzywiec.fooddelivery.bff.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DeliveryController {

    @PatchMapping("/deliveries/{orderId}")
    ResponseEntity<Void> updateADelivery(String orderId, @RequestBody UpdateDeliveryDTO updateOrder) {
        log.info("Received request to update a delivery for an '{}' order, update: {}", orderId, updateOrder);


        return ResponseEntity.accepted().build();
    }

    @PostMapping("/deliveries/{orderId}/delivery-man")
    ResponseEntity<Void> addTip(String orderId, @RequestBody ChangeDeliveryManDTO changeDeliveryMan) {
        log.info("Received request to assign '{}' delivery man to an '{}' order", changeDeliveryMan.getDeliveryManId(), orderId);


        return ResponseEntity.accepted().build();
    }
}
