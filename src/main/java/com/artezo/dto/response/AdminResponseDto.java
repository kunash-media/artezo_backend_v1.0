package com.artezo.dto.response;


import lombok.Builder;

@Builder
public class AdminResponseDto {


    private String adminId;             // STDXXXX
    private String adminFirstName;
    private String adminLastName;
    private String adminMobileNumber;
    private String adminAddress;
    private String adminRole;


    public AdminResponseDto(){}

    public AdminResponseDto(String adminId, String adminFirstName, String adminLastName,
                            String adminMobileNumber, String adminAddress, String adminRole) {
        this.adminId = adminId;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
        this.adminMobileNumber = adminMobileNumber;
        this.adminAddress = adminAddress;
        this.adminRole = adminRole;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAdminFirstName() {
        return adminFirstName;
    }

    public void setAdminFirstName(String adminFirstName) {
        this.adminFirstName = adminFirstName;
    }

    public String getAdminLastName() {
        return adminLastName;
    }

    public void setAdminLastName(String adminLastName) {
        this.adminLastName = adminLastName;
    }

    public String getAdminMobileNumber() {
        return adminMobileNumber;
    }

    public void setAdminMobileNumber(String adminMobileNumber) {
        this.adminMobileNumber = adminMobileNumber;
    }

    public String getAdminAddress() {
        return adminAddress;
    }

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }
}
