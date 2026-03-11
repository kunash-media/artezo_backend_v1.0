package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ShippingAddressRequestDTO;
import com.artezo.dto.response.ShippingAddressResponseDTO;
import com.artezo.entity.ShippingAddressEntity;
import com.artezo.entity.UserEntity;
import com.artezo.repository.ShippingAddressRepository;
import com.artezo.repository.UserRepository;
import com.artezo.service.ShippingAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingAddressServiceImpl implements ShippingAddressService {

    private static final Logger log = LoggerFactory.getLogger(ShippingAddressServiceImpl.class);

    @Autowired
    private ShippingAddressRepository shippingAddressRepository;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────
    // ADD ADDRESS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ShippingAddressResponseDTO addAddress(Long userId, ShippingAddressRequestDTO dto) {
        log.info("[ShippingAddress] addAddress() - userId={}", userId);

        UserEntity user = findUserOrThrow(userId);

        ShippingAddressEntity address = new ShippingAddressEntity();
        applyFields(address, dto);
        address.setUser(user);

        // If this is the user's first address OR caller explicitly set isDefault=true
        boolean isFirstAddress = shippingAddressRepository.countByUser(user) == 0;
        boolean requestedDefault = Boolean.TRUE.equals(dto.getIsDefault());

        if (isFirstAddress || requestedDefault) {
            log.info("[ShippingAddress] addAddress() - clearing previous defaults for userId={}", userId);
            shippingAddressRepository.clearDefaultForUser(user);
            address.setDefault(true);
        }

        ShippingAddressEntity saved = shippingAddressRepository.save(address);
        log.info("[ShippingAddress] addAddress() - saved shippingId={} for userId={}", saved.getShippingId(), userId);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // GET ALL ADDRESSES
    // ─────────────────────────────────────────────────────────────
    @Override
    public List<ShippingAddressResponseDTO> getAllAddresses(Long userId) {
        log.info("[ShippingAddress] getAllAddresses() - userId={}", userId);

        UserEntity user = findUserOrThrow(userId);
        List<ShippingAddressEntity> addresses = shippingAddressRepository.findByUser(user);

        log.info("[ShippingAddress] getAllAddresses() - found {} addresses for userId={}", addresses.size(), userId);
        return addresses.stream().map(this::toResponse).collect(Collectors.toList());
    }



    // ─────────────────────────────────────────────────────────────
    // GET ADDRESS BY ID
    // ─────────────────────────────────────────────────────────────
    @Override
    public ShippingAddressResponseDTO getAddressById(Long userId, Long shippingId) {
        log.info("[ShippingAddress] getAddressById() - userId={}, shippingId={}", userId, shippingId);

        ShippingAddressEntity address = findAddressOrThrow(shippingId, userId);

        log.info("[ShippingAddress] getAddressById() - found shippingId={}", shippingId);
        return toResponse(address);
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH ADDRESS (partial update — only non-null fields are applied)
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ShippingAddressResponseDTO patchAddress(Long userId, Long shippingId, ShippingAddressRequestDTO dto) {
        log.info("[ShippingAddress] patchAddress() - userId={}, shippingId={}", userId, shippingId);

        UserEntity user = findUserOrThrow(userId);
        ShippingAddressEntity address = findAddressOrThrow(shippingId, userId);

        // Track what changed for logging
        StringBuilder changes = new StringBuilder();

        if (dto.getCustomerName() != null) {
            changes.append("customerName ");
            address.setCustomerName(dto.getCustomerName());
        }
        if (dto.getCustomerPhone() != null) {
            changes.append("customerPhone ");
            address.setCustomerPhone(dto.getCustomerPhone());
        }
        if (dto.getCustomerEmail() != null) {
            changes.append("customerEmail ");
            address.setCustomerEmail(dto.getCustomerEmail());
        }
        if (dto.getFlatNo() != null) {
            changes.append("flatNo ");
            address.setFlatNo(dto.getFlatNo());
        }
        if (dto.getShippingAddress() != null) {
            changes.append("shippingAddress ");
            address.setShippingAddress(dto.getShippingAddress());
        }
        if (dto.getShippingCity() != null) {
            changes.append("shippingCity ");
            address.setShippingCity(dto.getShippingCity());
        }
        if (dto.getShippingState() != null) {
            changes.append("shippingState ");
            address.setShippingState(dto.getShippingState());
        }
        if (dto.getShippingPincode() != null) {
            changes.append("shippingPincode ");
            address.setShippingPincode(dto.getShippingPincode());
        }
        if (dto.getNearBy() != null) {
            changes.append("nearBy ");
            address.setNearBy(dto.getNearBy());
        }
        if (dto.getLandmark() != null) {
            changes.append("landmark ");
            address.setLandmark(dto.getLandmark());
        }

        // Handle isDefault flag change
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !address.isDefault()) {
            log.info("[ShippingAddress] patchAddress() - promoting shippingId={} to default for userId={}", shippingId, userId);
            shippingAddressRepository.clearDefaultForUser(user);
            address.setDefault(true);
            changes.append("isDefault ");
        }

        ShippingAddressEntity updated = shippingAddressRepository.save(address);
        log.info("[ShippingAddress] patchAddress() - updated shippingId={}, changed fields: [{}]",
                shippingId, changes.toString().trim());

        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────
    // SET DEFAULT ADDRESS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long shippingId) {
        log.info("[ShippingAddress] setDefaultAddress() - userId={}, shippingId={}", userId, shippingId);

        UserEntity user = findUserOrThrow(userId);
        ShippingAddressEntity address = findAddressOrThrow(shippingId, userId);

        shippingAddressRepository.clearDefaultForUser(user);
        address.setDefault(true);
        shippingAddressRepository.save(address);

        log.info("[ShippingAddress] setDefaultAddress() - shippingId={} set as default for userId={}", shippingId, userId);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE ADDRESS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteAddress(Long userId, Long shippingId) {
        log.info("[ShippingAddress] deleteAddress() - userId={}, shippingId={}", userId, shippingId);

        UserEntity user = findUserOrThrow(userId);
        ShippingAddressEntity address = findAddressOrThrow(shippingId, userId);

        boolean wasDefault = address.isDefault();
        shippingAddressRepository.delete(address);
        log.info("[ShippingAddress] deleteAddress() - deleted shippingId={}, wasDefault={}", shippingId, wasDefault);

        // If deleted address was the default, promote the next available one
        if (wasDefault) {
            List<ShippingAddressEntity> remaining = shippingAddressRepository.findByUser(user);
            if (!remaining.isEmpty()) {
                remaining.get(0).setDefault(true);
                shippingAddressRepository.save(remaining.get(0));
                log.info("[ShippingAddress] deleteAddress() - promoted shippingId={} as new default for userId={}",
                        remaining.get(0).getShippingId(), userId);
            } else {
                log.warn("[ShippingAddress] deleteAddress() - no remaining addresses for userId={}", userId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private UserEntity findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[ShippingAddress] findUserOrThrow() - userId={} not found", userId);
                    return new RuntimeException("User not found: " + userId);
                });
    }

    private ShippingAddressEntity findAddressOrThrow(Long shippingId, Long userId) {
        ShippingAddressEntity address = shippingAddressRepository.findById(shippingId)
                .orElseThrow(() -> {
                    log.error("[ShippingAddress] findAddressOrThrow() - shippingId={} not found", shippingId);
                    return new RuntimeException("Address not found: " + shippingId);
                });

        // Ownership check — prevent user A accessing user B's address
        if (!address.getUser().getUserId().equals(userId)) {
            log.error("[ShippingAddress] findAddressOrThrow() - shippingId={} does not belong to userId={}", shippingId, userId);
            throw new RuntimeException("Address does not belong to this user");
        }

        return address;
    }

    // Apply all non-null fields from DTO to entity (used by addAddress)
    private void applyFields(ShippingAddressEntity address, ShippingAddressRequestDTO dto) {
        if (dto.getCustomerName() != null)    address.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerPhone() != null)   address.setCustomerPhone(dto.getCustomerPhone());
        if (dto.getCustomerEmail() != null)   address.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getFlatNo() != null)          address.setFlatNo(dto.getFlatNo());
        if (dto.getShippingAddress() != null) address.setShippingAddress(dto.getShippingAddress());
        if (dto.getShippingCity() != null)    address.setShippingCity(dto.getShippingCity());
        if (dto.getShippingState() != null)   address.setShippingState(dto.getShippingState());
        if (dto.getShippingPincode() != null) address.setShippingPincode(dto.getShippingPincode());
        if (dto.getNearBy() != null)          address.setNearBy(dto.getNearBy());
        if (dto.getLandmark() != null)        address.setLandmark(dto.getLandmark());
    }

    private ShippingAddressResponseDTO toResponse(ShippingAddressEntity entity) {
        ShippingAddressResponseDTO dto = new ShippingAddressResponseDTO();
        dto.setShippingId(entity.getShippingId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setCustomerName(entity.getCustomerName());
        dto.setCustomerPhone(entity.getCustomerPhone());
        dto.setCustomerEmail(entity.getCustomerEmail());
        dto.setFlatNo(entity.getFlatNo());
        dto.setShippingAddress(entity.getShippingAddress());
        dto.setShippingCity(entity.getShippingCity());
        dto.setShippingState(entity.getShippingState());
        dto.setShippingPincode(entity.getShippingPincode());
        dto.setNearBy(entity.getNearBy());
        dto.setLandmark(entity.getLandmark());
        dto.setDefault(entity.isDefault());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        return dto;
    }
}