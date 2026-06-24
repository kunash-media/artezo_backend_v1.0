package com.artezo.enum_status;

/**
 * Lifecycle of a Shiprocket Hot Checkout attempt.
 *
 * PENDING   → SRPreCheckoutEntity created, user redirected to SR window
 * CONFIRMED → SR /order-sync fired, OrderEntity saved in DB
 * CANCELLED → User hit cancel_url (SR redirects back with ?status=cancelled)
 * ABANDONED → No SR callback within TTL — marked by a scheduled batch job
 * FAILED    → SR callback received but our order-creation threw an exception
 */
public enum SRCheckoutStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    ABANDONED,
    FAILED
}