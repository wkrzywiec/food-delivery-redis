package io.wkrzywiec.fooddelivery.domain.delivery;

class DeliveryException extends RuntimeException {

    DeliveryException(String message) {
        super(message);
    }

    DeliveryException(String message, Exception cause) {
        super(message, cause);
    }
}
