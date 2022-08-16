package io.wkrzywiec.fooddelivery.bff.controller;

import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import io.wkrzywiec.fooddelivery.bff.controller.model.ChangeDeliveryManDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.ResponseDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.UpdateDeliveryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final InboxPublisher inboxPublisher;
    private static final String DELIVERY_INBOX = "delivery-inbox";

    @PatchMapping("/deliveries/{orderId}")
    ResponseEntity<ResponseDTO> updateADelivery(String orderId, @RequestBody UpdateDeliveryDTO updateDelivery) {
        log.info("Received request to update a delivery for an '{}' order, update: {}", orderId, updateDelivery);
        updateDelivery.setOrderId(orderId);
        inboxPublisher.storeMessage(DELIVERY_INBOX + ":update", updateDelivery);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }

    @PostMapping("/deliveries/{orderId}/delivery-man")
    ResponseEntity<ResponseDTO> addTip(String orderId, @RequestBody ChangeDeliveryManDTO changeDeliveryMan) {
        log.info("Received request to assign '{}' delivery man to an '{}' order", changeDeliveryMan.getDeliveryManId(), orderId);
        changeDeliveryMan.setOrderId(orderId);
        inboxPublisher.storeMessage(DELIVERY_INBOX + ":delivery-man", changeDeliveryMan);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }
}
