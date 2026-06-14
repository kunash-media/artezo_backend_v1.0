package com.artezo.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShiprocketOrderPayload(

        // SR's internal order ID
        @JsonProperty("id") String srOrderId,

        // Your order reference (order_id you passed when opening checkout)
        @JsonProperty("name") String orderName,

        // Customer info
        @JsonProperty("customer") CustomerInfo customer,

        // Shipping address chosen by customer in SR checkout
        @JsonProperty("shipping_address") Address shippingAddress,

        // Billing address
        @JsonProperty("billing_address") Address billingAddress,

        // Line items
        @JsonProperty("line_items") List<LineItem> lineItems,

        // Financials
        @JsonProperty("total_price") String totalPrice,
        @JsonProperty("subtotal_price") String subtotalPrice,
        @JsonProperty("total_tax") String totalTax,
        @JsonProperty("total_discounts") String totalDiscounts,

        // Payment
        @JsonProperty("financial_status") String financialStatus,  // "paid" | "pending" | "cod"
        @JsonProperty("payment_gateway") String paymentGateway,    // "razorpay" | "cod" etc.
        @JsonProperty("gateway_order_id") String gatewayOrderId,   // Razorpay order ID

        // Fulfillment
        @JsonProperty("fulfillment_status") String fulfillmentStatus,

        // Source tag you passed
        @JsonProperty("source_name") String sourceName,

        @JsonProperty("created_at") String createdAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerInfo(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("email") String email,
            @JsonProperty("phone") String phone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("address1") String address1,
            @JsonProperty("address2") String address2,
            @JsonProperty("city") String city,
            @JsonProperty("province") String province,
            @JsonProperty("zip") String zip,
            @JsonProperty("country") String country,
            @JsonProperty("phone") String phone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineItem(
            @JsonProperty("id") String itemId,
            @JsonProperty("title") String title,
            @JsonProperty("sku") String sku,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("price") String price,
            @JsonProperty("variant_title") String variantTitle
    ) {}
}