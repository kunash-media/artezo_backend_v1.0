package com.artezo.controller;

import com.artezo.dto.request.ShippingAddressRequestDTO;
import com.artezo.dto.response.ShippingAddressResponseDTO;
import com.artezo.service.ShippingAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping-addresses")
public class ShippingAddressController {

    private static final Logger log = LoggerFactory.getLogger(ShippingAddressController.class);

    @Autowired
    private ShippingAddressService shippingAddressService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/users/{userId}/addresses
    // Add a new shipping address
    // ─────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> addAddress(
            @PathVariable Long userId,
            @RequestBody ShippingAddressRequestDTO dto) {

        log.info("[Controller] POST /api/users/{}/addresses - addAddress called", userId);
        try {
            ShippingAddressResponseDTO response = shippingAddressService.addAddress(userId, dto);
            log.info("[Controller] addAddress - success, shippingId={} for userId={}", response.getShippingId(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("[Controller] addAddress - failed for userId={}, reason={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/users/{userId}/addresses
    // Get all addresses for a user
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllAddresses(@PathVariable Long userId) {

        log.info("[Controller] GET /api/users/{}/addresses - getAllAddresses called", userId);
        try {
            List<ShippingAddressResponseDTO> addresses = shippingAddressService.getAllAddresses(userId);
            log.info("[Controller] getAllAddresses - returning {} addresses for userId={}", addresses.size(), userId);
            return ResponseEntity.ok(addresses);
        } catch (RuntimeException e) {
            log.error("[Controller] getAllAddresses - failed for userId={}, reason={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // ─────────────────────────────────────────────────────────────
    // GET /api/users/{userId}/addresses/{shippingId}
    // Get a specific address by ID
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{shippingId}")
    public ResponseEntity<?> getAddressById(
            @PathVariable Long userId,
            @PathVariable Long shippingId) {

        log.info("[Controller] GET /api/users/{}/addresses/{} - getAddressById called", userId, shippingId);
        try {
            ShippingAddressResponseDTO address = shippingAddressService.getAddressById(userId, shippingId);
            log.info("[Controller] getAddressById - found shippingId={} for userId={}", shippingId, userId);
            return ResponseEntity.ok(address);
        } catch (RuntimeException e) {
            log.error("[Controller] getAddressById - failed, userId={}, shippingId={}, reason={}",
                    userId, shippingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/users/{userId}/addresses/{shippingId}
    // Partial update — only provided fields are updated
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/{shippingId}")
    public ResponseEntity<?> patchAddress(
            @PathVariable Long userId,
            @PathVariable Long shippingId,
            @RequestBody ShippingAddressRequestDTO dto) {

        log.info("[Controller] PATCH /api/users/{}/addresses/{} - patchAddress called", userId, shippingId);
        try {
            ShippingAddressResponseDTO updated = shippingAddressService.patchAddress(userId, shippingId, dto);
            log.info("[Controller] patchAddress - success, shippingId={} for userId={}", shippingId, userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("[Controller] patchAddress - failed, userId={}, shippingId={}, reason={}",
                    userId, shippingId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/users/{userId}/addresses/{shippingId}/set-default
    // Set an address as default (dedicated endpoint for clarity)
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/{shippingId}/set-default")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable Long userId,
            @PathVariable Long shippingId) {

        log.info("[Controller] PATCH /api/users/{}/addresses/{}/set-default - setDefaultAddress called", userId, shippingId);
        try {
            shippingAddressService.setDefaultAddress(userId, shippingId);
            log.info("[Controller] setDefaultAddress - shippingId={} set as default for userId={}", shippingId, userId);
            return ResponseEntity.ok(Map.of("message", "Default address updated successfully"));
        } catch (RuntimeException e) {
            log.error("[Controller] setDefaultAddress - failed, userId={}, shippingId={}, reason={}",
                    userId, shippingId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/users/{userId}/addresses/{shippingId}
    // Delete an address (auto-promotes next address as default if deleted was default)
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/{shippingId}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long shippingId) {

        log.info("[Controller] DELETE /api/users/{}/addresses/{} - deleteAddress called", userId, shippingId);
        try {
            shippingAddressService.deleteAddress(userId, shippingId);
            log.info("[Controller] deleteAddress - success, shippingId={} for userId={}", shippingId, userId);
            return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
        } catch (RuntimeException e) {
            log.error("[Controller] deleteAddress - failed, userId={}, shippingId={}, reason={}",
                    userId, shippingId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}