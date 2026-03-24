package com.artezo.service;


import com.artezo.dto.request.OtpVerificationDto;

public interface OtpService {

    void sendOtpEmail(String email);

    // ✅ separated concerns
    boolean verifyOtp(String email, String rawOtp);

    boolean resetPassword(String email, String rawOtp, String newPassword);


}