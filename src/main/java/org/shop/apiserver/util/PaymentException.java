package org.shop.apiserver.util;

public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}