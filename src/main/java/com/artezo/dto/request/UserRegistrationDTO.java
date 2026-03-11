package com.artezo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationDTO {

    // ── Name fields ──────────────────────────────────────────────
    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    private String firstName;

    // Optional — middle name
    @Size(max = 50, message = "Middle name must be under 50 characters")
    private String middleName;

    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    private String lastName;

    // ── Contact fields ───────────────────────────────────────────
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    @Size(min = 6, message = "Password must be at least 8 characters")
    private String password;

    // ── Address fields (for default shipping address) ─────────────
    private String address;

    private String flatNo;

    private String city;

    private String state;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode")
    private String pincode;

    private String nearBy;

    private String landmark;

    // ── Helper: build combined full name for shipping label ────────
    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return firstName.trim() + " " + middleName.trim() + " " + lastName.trim();
        }
        return firstName.trim() + " " + lastName.trim();
    }

    // Getters & Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getFlatNo() { return flatNo; }
    public void setFlatNo(String flatNo) { this.flatNo = flatNo; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getNearBy() { return nearBy; }
    public void setNearBy(String nearBy) { this.nearBy = nearBy; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
}