package com.artezo.dto.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShiprocketWebhookPayload(
        @JsonProperty("event") String event,         // "ORDER_PLACED", "PAYMENT_SUCCESS" etc.
        @JsonProperty("order_id") String orderId,
        @JsonProperty("sr_order_id") String srOrderId,
        @JsonProperty("status") String status,
        @JsonProperty("payment_status") String paymentStatus,
        @JsonProperty("awb") String awb,             // tracking number, populated on shipping
        @JsonProperty("courier") String courier,
        @JsonProperty("timestamp") String timestamp
) {}