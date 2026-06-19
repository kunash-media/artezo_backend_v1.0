package com.artezo.util;


import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// In-memory cache — holds address for ~10 minutes until confirm call arrives
// Keyed by razorpay_order_id
@Component
public class MagicCheckoutAddressCache {

    // orderId → address map
    private final Map<String, MagicAddressData> cache = new ConcurrentHashMap<>();

    public void store(String razorpayOrderId, MagicAddressData data) {
        cache.put(razorpayOrderId, data);
    }

    public Optional<MagicAddressData> get(String razorpayOrderId) {
        return Optional.ofNullable(cache.get(razorpayOrderId));
    }

    public void remove(String razorpayOrderId) {
        cache.remove(razorpayOrderId);
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────
    public static class MagicAddressData {
        public String name;
        public String email;
        public String phone;
        public String address1;
        public String address2;
        public String city;
        public String state;
        public String pincode;
        public String paymentMethod;  // PREPAID or COD
    }
}