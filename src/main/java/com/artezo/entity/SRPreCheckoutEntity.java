package com.artezo.entity;

import com.artezo.enum_status.SRCheckoutStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks every "Buy Now → SR Checkout" attempt.
 *
 * Created BEFORE the user is redirected to SR (from /api/shiprocket/pre-checkout).
 * Updated AFTER SR posts back to /api/shiprocket/order-sync.
 *
 * This table is SEPARATE from OrderEntity intentionally:
 *  - Captures abandoned checkouts (user never completed payment)
 *  - One-to-one with OrderEntity once SR confirms (via orderId FK)
 *  - Zero coupling with your existing cart-based order flow
 */
@Entity
@Table(
        name = "sr_pre_checkout",
        indexes = {
                @Index(name = "idx_sr_pre_order_ref",   columnList = "orderRef",   unique = true),
                @Index(name = "idx_sr_pre_status",       columnList = "status"),
                @Index(name = "idx_sr_pre_sr_order_id",  columnList = "srOrderId")
        }
)
public class SRPreCheckoutEntity {

    // ── IDENTITY ─────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order reference YOUR frontend generated (ORD-1234567890-ABCD).
     * This is the `order_id` param passed to SR Checkout URL.
     * SR echoes it back as `name` in the order-sync payload.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String orderRef;

    // ── STATUS ────────────────────────────────────────────────────────────────

    /**
     * PENDING   → created before SR redirect
     * CONFIRMED → SR order-sync received, OrderEntity created
     * CANCELLED → user hit cancel_url
     * ABANDONED → no SR callback within TTL (batch job marks these)
     * FAILED    → SR callback received but order creation failed
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SRCheckoutStatus status = SRCheckoutStatus.PENDING;

    // ── PRODUCT SNAPSHOT (from frontend at time of Buy Now click) ─────────────

    @Column(nullable = false, length = 30)
    private String productStrId;        // PRD00001

    @Column(nullable = false, length = 200)
    private String productName;         // snapshot incl. variant label

    private String variantId;           // VAR-GOLD — null if no variant
    private String variantLabel;        // "Medium (9\")" — display string

    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;           // selling price per unit

    @Column(nullable = false)
    private Double mrp;                 // MRP per unit

    @Column(nullable = false)
    private Double totalAmount;         // unitPrice × quantity

    // ── SR RESPONSE DATA (populated after order-sync POST) ────────────────────

    /**
     * SR's internal numeric/string order ID — set after /order-sync fires.
     * null until SR confirms.
     */
    @Column(length = 60)
    private String srOrderId;

    /**
     * FK to the OrderEntity created from SR's order-sync payload.
     * null until order is confirmed and persisted.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private OrderEntity order;

    // ── CUSTOMER (populated after order-sync, from SR payload) ───────────────

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // ── PAYMENT (populated after order-sync) ─────────────────────────────────

    @Column(length = 20)
    private String financialStatus;     // "paid" | "pending" | "cod"

    @Column(length = 40)
    private String paymentGateway;      // "razorpay" | "cod"

    @Column(length = 100)
    private String gatewayOrderId;      // Razorpay order ID if prepaid

    // ── METADATA ─────────────────────────────────────────────────────────────

    @Column(length = 20)
    private String source = "SR_HOT_CHECKOUT";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Timestamp when SR order-sync was received */
    private LocalDateTime confirmedAt;

    /** Timestamp when user hit cancel_url */
    private LocalDateTime cancelledAt;

    // ── CONSTRUCTORS ─────────────────────────────────────────────────────────

    public SRPreCheckoutEntity() {}

    // ── GETTERS & SETTERS ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderRef() { return orderRef; }
    public void setOrderRef(String orderRef) { this.orderRef = orderRef; }

    public SRCheckoutStatus getStatus() { return status; }
    public void setStatus(SRCheckoutStatus status) { this.status = status; }

    public String getProductStrId() { return productStrId; }
    public void setProductStrId(String productStrId) { this.productStrId = productStrId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }

    public String getVariantLabel() { return variantLabel; }
    public void setVariantLabel(String variantLabel) { this.variantLabel = variantLabel; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getMrp() { return mrp; }
    public void setMrp(Double mrp) { this.mrp = mrp; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getSrOrderId() { return srOrderId; }
    public void setSrOrderId(String srOrderId) { this.srOrderId = srOrderId; }

    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getFinancialStatus() { return financialStatus; }
    public void setFinancialStatus(String financialStatus) { this.financialStatus = financialStatus; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getGatewayOrderId() { return gatewayOrderId; }
    public void setGatewayOrderId(String gatewayOrderId) { this.gatewayOrderId = gatewayOrderId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}