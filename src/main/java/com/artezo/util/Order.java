package com.artezo.util;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hot_checkout_orders")
@Getter @Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderRef;          // Your internal ref (passed to SR)

    @Column(unique = true)
    private String srOrderId;         // Shiprocket's order ID

    private String razorpayOrderId;   // Razorpay txn ID (if prepaid)
    private String trackingAwb;       // AWB number after shipping
    private String courierName;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    @Column(length = 500)
    private String shippingAddress;
    private String shippingCity;
    private String shippingPincode;
    private String shippingState;

    private String productName;
    private String productSku;
    private Integer quantity;

    private BigDecimal totalAmount;
    private String paymentMethod;     // "razorpay" | "cod" | "upi"

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String source;            // "SHIPROCKET_CHECKOUT"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}