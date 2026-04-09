package com.artezo.dto.request;

public class UserLoginRequest {

    private String identifier;   // accepts email OR phone
    private String password;

    public UserLoginRequest() {}

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}