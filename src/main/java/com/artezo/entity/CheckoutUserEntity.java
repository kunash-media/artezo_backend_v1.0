package com.artezo.entity;

import com.artezo.enum_status.CheckoutUserSource;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_users")
public class CheckoutUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkoutUserId;

    // Primary identity — phone from Razorpay modal
    @Column(unique = true, nullable = false, length = 20)
    private String phone;

    private String name;

    @Column(length = 255)
    private String email;

    // Last known address snapshot — updated on every new order
    @Column(name = "last_address1", length = 500)
    private String lastAddress1;

    @Column(name = "last_address2", length = 500)
    private String lastAddress2;

    @Column(name = "last_city", length = 100)
    private String lastCity;

    @Column(name = "last_state", length = 100)
    private String lastState;

    @Column(name = "last_pincode", length = 20)
    private String lastPincode;

    @Column(name = "last_country", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'India'")
    private String lastCountry = "India";

    // Auto-linked if a registered user has same phone — null until then
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id", nullable = true)
    private UserEntity linkedUser;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CheckoutUserSource source;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public CheckoutUserEntity() {}

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Long getCheckoutUserId() { return checkoutUserId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLastAddress1() { return lastAddress1; }
    public void setLastAddress1(String lastAddress1) { this.lastAddress1 = lastAddress1; }

    public String getLastAddress2() { return lastAddress2; }
    public void setLastAddress2(String lastAddress2) { this.lastAddress2 = lastAddress2; }

    public String getLastCity() { return lastCity; }
    public void setLastCity(String lastCity) { this.lastCity = lastCity; }

    public String getLastState() { return lastState; }
    public void setLastState(String lastState) { this.lastState = lastState; }

    public String getLastPincode() { return lastPincode; }
    public void setLastPincode(String lastPincode) { this.lastPincode = lastPincode; }

    public String getLastCountry() { return lastCountry; }
    public void setLastCountry(String lastCountry) { this.lastCountry = lastCountry; }

    public UserEntity getLinkedUser() { return linkedUser; }
    public void setLinkedUser(UserEntity linkedUser) { this.linkedUser = linkedUser; }

    public CheckoutUserSource getSource() { return source; }
    public void setSource(CheckoutUserSource source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}