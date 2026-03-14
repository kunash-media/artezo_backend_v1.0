package com.artezo.enum_status;

public enum OrderFlow {

    BUY_NOW,    // Shiprocket Checkout widget — SR handles payment + shipment
    CART        // Your checkout page — your backend calls SR create order API
}
