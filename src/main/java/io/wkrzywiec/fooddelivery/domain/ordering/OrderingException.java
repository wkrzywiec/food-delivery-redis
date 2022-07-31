package io.wkrzywiec.fooddelivery.domain.ordering;

class OrderingException extends RuntimeException {

    OrderingException(String message) {
        super(message);
    }

    OrderingException(String message, Exception cause) {
        super(message, cause);
    }
}
