package com.artezo.service.serviceImpl;


import com.artezo.bcrypt.BcryptEncoderConfig;
import com.artezo.entity.AdminEntity;
import com.artezo.entity.OtpEntity;
import com.artezo.entity.UserEntity;
import com.artezo.enum_status.OtpPurpose;
import com.artezo.exceptions.ResourceNotFoundException;
import com.artezo.exceptions.TooManyRequestsException;
import com.artezo.repository.AdminRepository;
import com.artezo.repository.OtpRepository;
import com.artezo.repository.UserRepository;
import com.artezo.service.EmailService;
import com.artezo.service.OtpService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;
import java.util.Random;

@Service
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final OtpRepository otpRepository;
    private final BcryptEncoderConfig passwordEncoder;
    private final EmailService emailService;

    public OtpServiceImpl(UserRepository userRepository, AdminRepository adminRepository,
                          OtpRepository otpRepository, BcryptEncoderConfig passwordEncoder,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void sendOtpEmail(String email) {
        // 1. Validate email — user first, admin fallback
        UserEntity userEntity = null;
        AdminEntity adminEntity = null;

        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            userEntity = user.get();
        } else {
            adminEntity = adminRepository.findByAdminEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No account: " + email));
        }

        // 2. Rate limit check
        otpRepository.findLatestByEmail(email).ifPresent(existing -> {
            if (existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new TooManyRequestsException("Please wait before requesting another OTP.");
            }
        });

        // 3. Save OTP in transaction
        String rawOtp = persistOtp(email, userEntity, adminEntity);

        // 4. Send email OUTSIDE transaction
        emailService.sendOtpEmail(email, "Your OTP for Password Reset",
                "Your OTP is: " + rawOtp + ". Valid for 5 minutes.");
    }

    @Transactional
    public String persistOtp(String email, UserEntity user, AdminEntity admin) {
        otpRepository.deleteByEmail(email);
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        otpRepository.save(new OtpEntity(
                user, admin, passwordEncoder.encode(otp), null,
                LocalDateTime.now().plusMinutes(5), email, OtpPurpose.PASSWORD_RESET
        ));
        return otp;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String rawOtp) {
        List<OtpEntity> validOtps = otpRepository.findValidEmailOtps(email, LocalDateTime.now());
        if (validOtps.isEmpty()) return false;
        return otpMatches(validOtps, rawOtp);
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String rawOtp, String newPassword) {
        List<OtpEntity> validOtps = otpRepository.findValidEmailOtps(email, LocalDateTime.now());
        if (validOtps.isEmpty() || !otpMatches(validOtps, rawOtp)) return false;

        // Delete OTPs FIRST
        otpRepository.deleteAll(validOtps);

        // Then update password
        String encoded = passwordEncoder.encode(newPassword);
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            user.get().setPasswordHash(encoded);
            userRepository.save(user.get());
        } else {
            AdminEntity admin = adminRepository.findByAdminEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("No account: " + email));
            admin.setAdminPassword(encoded);
            adminRepository.save(admin);
        }
        return true;
    }

    private boolean otpMatches(List<OtpEntity> otps, String rawOtp) {
        return otps.stream()
                .anyMatch(otp -> passwordEncoder.matches(rawOtp, otp.getOtpCode()));
    }
}
