package com.artezo.service.serviceImpl;

import com.artezo.dto.request.PaymentRequest;
import com.artezo.dto.request.PaymentVerificationRequest;
import com.artezo.dto.response.PaymentResponse;
import com.artezo.dto.response.PaymentVerificationResponse;
import com.artezo.entity.PaymentOrder;
import com.artezo.entity.UserEntity;
import com.artezo.enum_status.PaymentStatus;
import com.artezo.repository.PaymentOrderRepository;
import com.artezo.repository.UserRepository;
import com.artezo.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.micrometer.common.util.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {


    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    public PaymentServiceImpl(PaymentOrderRepository paymentOrderRepository, UserRepository userRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.userRepository = userRepository;
    }

    @Value("${razorpay.key_id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret:}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    // Initialize Razorpay client
    private RazorpayClient getRazorpayClient() throws RazorpayException {
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        }
        return razorpayClient;
    }

    @Override
    public PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception {

        try {
            // Validate userId and get user
            if (request.getUserId() == null) {
                throw new Exception("User ID is required");
            }

            UserEntity user = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new Exception("User not found with ID: " + request.getUserId()));

            // Add logging to verify keys are loaded
            log.info("Attempting to create Razorpay order for userId: {}", request.getUserId());
            log.debug("Razorpay Key ID: {}", razorpayKeyId);
            log.debug("Razorpay Key Secret present: {}", razorpayKeySecret != null ? "YES" : "NO");

            if (StringUtils.isBlank(razorpayKeyId) || StringUtils.isBlank(razorpayKeySecret)) {
                log.error("Razorpay keys are not properly configured!");
                throw new Exception("Razorpay credentials not configured");
            }

            // Convert amount to paise for Razorpay
            long amountInPaise = Math.round(request.getAmount() * 100);

            // Validate minimum amount (Razorpay minimum is typically 100 paise = ₹1)
            if(amountInPaise < 100) {
                throw new Exception("Amount must be at least ₹1");
            }

            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);    // Convert to paise
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getReceipt());

            // Add customer notes
            JSONObject notes = new JSONObject();
            notes.put("customer_name", request.getCustomerName());
            notes.put("customer_email", request.getCustomerEmail());
            notes.put("customer_phone", request.getCustomerPhone());
            notes.put("user_id", request.getUserId().toString());
            orderRequest.put("notes", notes);

            Order razorpayOrder = getRazorpayClient().orders.create(orderRequest);

            // Save to database
            PaymentOrder paymentOrder = new PaymentOrder();
            paymentOrder.setUser(user); // Set the user entity
            paymentOrder.setRazorpayOrderId(razorpayOrder.get("id"));
            paymentOrder.setAmount(request.getAmount().intValue());      // Store original amount
            paymentOrder.setCurrency(request.getCurrency());
            paymentOrder.setReceipt(request.getReceipt());
            paymentOrder.setCustomerName(request.getCustomerName());
            paymentOrder.setCustomerEmail(request.getCustomerEmail());
            paymentOrder.setCustomerPhone(request.getCustomerPhone());
            paymentOrder.setStatus(PaymentStatus.CREATED);

            paymentOrderRepository.save(paymentOrder);

            // Create response
            PaymentResponse response = new PaymentResponse();
            response.setRazorpayOrderId(razorpayOrder.get("id"));
            response.setAmount(request.getAmount());
            response.setCurrency(request.getCurrency());
            response.setReceipt(request.getReceipt());
            response.setStatus(razorpayOrder.get("status"));
            response.setCustomerName(request.getCustomerName());
            response.setCustomerEmail(request.getCustomerEmail());
            response.setCustomerPhone(request.getCustomerPhone());
            response.setRazorpayKeyId(razorpayKeyId);

            log.info("Payment order created successfully for userId: {} with orderId: {}",
                    request.getUserId(), razorpayOrder.get("id"));
            return response;

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new Exception("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) throws Exception {
        try {
            // Get payment order from database
            PaymentOrder paymentOrder = paymentOrderRepository
                    .findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new Exception("Payment order not found"));

            // Verify signature
            boolean isValidSignature = verifyRazorpaySignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );

            PaymentVerificationResponse response = new PaymentVerificationResponse();

            if (isValidSignature) {
                // Update payment order
                paymentOrder.setRazorpayPaymentId(request.getRazorpayPaymentId());
                paymentOrder.setRazorpaySignature(request.getRazorpaySignature());
                paymentOrder.setStatus(PaymentStatus.PAID);
                paymentOrderRepository.save(paymentOrder);

                response.setSuccess(true);
                response.setMessage("Payment verified successfully");
                response.setStatus("PAID");

                log.info("Payment verified successfully for userId: {} with paymentId: {}",
                        paymentOrder.getUser().getUserId(), request.getRazorpayPaymentId());
            } else {
                paymentOrder.setStatus(PaymentStatus.FAILED);
                paymentOrderRepository.save(paymentOrder);

                response.setSuccess(false);
                response.setMessage("Payment verification failed");
                response.setStatus("FAILED");

                log.error("Payment verification failed for order: {}", request.getRazorpayOrderId());
            }

            response.setPaymentId(request.getRazorpayPaymentId());
            response.setOrderId(request.getRazorpayOrderId());

            return response;

        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            throw new Exception("Payment verification failed: " + e.getMessage());
        }
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = calculateHmacSha256(payload, razorpayKeySecret);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }

    private String calculateHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public PaymentOrder getPaymentOrderByRazorpayId(String razorpayOrderId) {
        return paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment order not found: " + razorpayOrderId));
    }

    @Override
    public List<PaymentOrder> getAllPaymentOrders() {
        return paymentOrderRepository.findAll();
    }

    @Override
    public PaymentOrder updatePaymentStatus(String razorpayOrderId, String status) {
        PaymentOrder paymentOrder = getPaymentOrderByRazorpayId(razorpayOrderId);
        paymentOrder.setStatus(PaymentStatus.valueOf(status.toUpperCase()));
        return paymentOrderRepository.save(paymentOrder);
    }

    // New methods for user-based operations
    @Override
    public List<PaymentOrder> getPaymentOrdersByUserId(Long userId) {
        // Validate user exists
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return paymentOrderRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public PaymentOrder getPaymentOrderByRazorpayIdAndUserId(String razorpayOrderId, Long userId) {
        // Validate user exists
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return paymentOrderRepository.findByRazorpayOrderIdAndUserUserId(razorpayOrderId, userId)
                .orElseThrow(() -> new RuntimeException("Payment order not found for user: " + userId +
                        " with orderId: " + razorpayOrderId));
    }

    @Override
    public List<PaymentOrder> getPaymentOrdersByUserIdAndStatus(Long userId, String status) {
        // Validate user exists
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid payment status: " + status);
        }

        return paymentOrderRepository.findByUserUserIdAndStatus(userId, paymentStatus);
    }
}