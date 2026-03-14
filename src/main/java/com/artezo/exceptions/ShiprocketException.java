package com.artezo.exceptions;

class ShiprocketException extends RuntimeException {

    private final String errorCode;

    public ShiprocketException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShiprocketException(String message) {
        this(message, "SHIPROCKET_ERROR");
    }

    public String getErrorCode() { return errorCode; }
}