package io.wkrzywiec.fooddelivery.delivery;

class DeliveryException extends RuntimeException {

    DeliveryException(String message) {
        super(message);
    }

    DeliveryException(String message, Exception cause) {
        super(message, cause);
    }
}
