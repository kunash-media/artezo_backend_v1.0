package com.artezo.dto.response;

import com.artezo.enum_status.UserStatus;

import java.math.BigDecimal;

public class UserResponseDTO {

    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String createdAt;

    private Long totalOrdersCount;     // total number of orders placed by this customer
    private BigDecimal totalSpent;

    private UserStatus userStatus;


    // Default shipping address snapshot (optional, returned on registration)
    private Long defaultShippingAddressId;

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Long getDefaultShippingAddressId() { return defaultShippingAddressId; }
    public void setDefaultShippingAddressId(Long defaultShippingAddressId) {
        this.defaultShippingAddressId = defaultShippingAddressId;
    }

    public Long getTotalOrdersCount() {
        return totalOrdersCount;
    }

    public void setTotalOrdersCount(Long totalOrdersCount) {
        this.totalOrdersCount = totalOrdersCount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }
}