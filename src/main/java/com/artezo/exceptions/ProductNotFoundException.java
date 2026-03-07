package com.artezo.exceptions;

public class ProductNotFoundException extends Throwable {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
