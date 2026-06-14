package com.artezo.util;

public enum OrderStatus {
    PENDING,      // Pre-checkout stub created
    CONFIRMED,    // SR confirmed order placed (COD)
    PAID,         // Payment successful (prepaid)
    SHIPPED,      // AWB assigned, handed to courier
    DELIVERED,    // Delivered to customer
    CANCELLED,    // Cancelled
    RTO           // Return to origin
}