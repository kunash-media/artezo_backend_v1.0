package com.artezo.exceptions;

public class OrderException extends RuntimeException {

    private final String errorCode;

    public OrderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderException(String message) {
        this(message, "ORDER_ERROR");
    }

    public String getErrorCode() { return errorCode; }
}
