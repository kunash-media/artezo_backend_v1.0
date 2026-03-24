package com.artezo.entity;

import com.artezo.enum_status.OtpPurpose;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_table")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private AdminEntity admin;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "mobile_number")
    private String mobile;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Column(name = "email")
    private String email;


    @Column(name = "purpose")
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose; // "PASSWORD_RESET", "VERIFICATION", etc.


    public OtpEntity(){}


    public OtpEntity(Long id, UserEntity user, AdminEntity admin, String otpCode, String mobile, LocalDateTime createdAt, LocalDateTime expiresAt, boolean isUsed, String email, OtpPurpose purpose) {
        this.id = id;
        this.user = user;
        this.admin = admin;
        this.otpCode = otpCode;
        this.mobile = mobile;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isUsed = isUsed;
        this.email = email;
        this.purpose = purpose;
    }

    public OtpEntity(UserEntity user, AdminEntity admin, String otpCode, String mobile,
                     LocalDateTime expiresAt, String email, OtpPurpose purpose) {
        this.user = user;
        this.admin = admin;
        this.otpCode = otpCode;
        this.mobile = mobile;
        this.expiresAt = expiresAt;
        this.email = email;
        this.purpose = purpose;
        this.createdAt = LocalDateTime.now();
        this.isUsed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public AdminEntity getAdmin() {
        return admin;
    }

    public void setAdmin(AdminEntity admin) {
        this.admin = admin;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getMobileNumber() {
        return mobile;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobile = mobileNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}