package com.artezo.controller;

import com.artezo.dto.request.EmailRequest;
import com.artezo.dto.request.OtpVerificationDto;
import com.artezo.dto.request.ResetPasswordRequest;
import com.artezo.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;
    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    // ✅ constructor injection, not @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(@RequestBody EmailRequest emailRequest) {
        otpService.sendOtpEmail(emailRequest.getEmail());
        return ResponseEntity.ok("OTP sent to: " + emailRequest.getEmail());
        // ✅ let GlobalExceptionHandler handle exceptions — no try/catch here
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateOtp(@RequestBody OtpVerificationDto dto) {
        boolean valid = otpService.verifyOtp(dto.getEmail(), dto.getOtp());
        if (valid) return ResponseEntity.ok("OTP is valid");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest req) {
        logger.info("Password reset requested for: {}", req.getEmail());
        boolean success = otpService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
        if (success) {
            logger.info("Password reset successful for: {}", req.getEmail());
            return ResponseEntity.ok("Password reset successfully");
        }
        logger.warn("Invalid OTP for reset: {}", req.getEmail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
    }
}