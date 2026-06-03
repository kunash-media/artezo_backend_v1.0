package com.artezo.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "coupon_user_usage")
public class CouponUserUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    // ---- Constructors ----
    public CouponUserUsage() {}

    public CouponUserUsage(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.usedCount = 0;
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
}