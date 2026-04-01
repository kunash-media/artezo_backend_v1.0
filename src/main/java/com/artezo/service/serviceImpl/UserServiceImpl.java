package com.artezo.service.serviceImpl;

import com.artezo.bcrypt.BcryptEncoderConfig;
import com.artezo.dto.request.UserPatchDTO;
import com.artezo.dto.request.UserRegistrationDTO;
import com.artezo.dto.response.UserResponseDTO;
import com.artezo.dto.stats.orders.OrderStats;
import com.artezo.entity.ShippingAddressEntity;
import com.artezo.entity.UserEntity;
import com.artezo.repository.OrderRepository;
import com.artezo.repository.ShippingAddressRepository;
import com.artezo.repository.UserRepository;
import com.artezo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);


    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final BcryptEncoderConfig passwordEncoder;
    private final OrderRepository orderRepository;

    public UserServiceImpl(UserRepository userRepository, ShippingAddressRepository shippingAddressRepository, BcryptEncoderConfig passwordEncoder, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public UserResponseDTO registerUser(UserRegistrationDTO dto) {
        log.info("[UserService] registerUser() - email={}", dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("[UserService] registerUser() - email already exists: {}", dto.getEmail());
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            log.warn("[UserService] registerUser() - phone already exists: {}", dto.getPhone());
            throw new RuntimeException("Phone number already registered");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(dto.getFirstName());
        user.setMiddleName(dto.getMiddleName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        UserEntity savedUser = userRepository.save(user);
        log.info("[UserService] registerUser() - user saved, userId={}", savedUser.getUserId());

        // Auto-create default shipping address from registration data
        ShippingAddressEntity defaultAddress = new ShippingAddressEntity();
        defaultAddress.setUser(savedUser);
        defaultAddress.setCustomerName(dto.getFullName());
        defaultAddress.setCustomerPhone(dto.getPhone());
        defaultAddress.setCustomerEmail(dto.getEmail());
        defaultAddress.setFlatNo(dto.getFlatNo());
        defaultAddress.setShippingAddress(dto.getAddress());
        defaultAddress.setShippingCity(dto.getCity());
        defaultAddress.setShippingState(dto.getState());
        defaultAddress.setShippingPincode(dto.getPincode());
        defaultAddress.setNearBy(dto.getNearBy());
        defaultAddress.setLandmark(dto.getLandmark());
        defaultAddress.setDefault(true);
        ShippingAddressEntity savedAddress = shippingAddressRepository.save(defaultAddress);
        log.info("[UserService] registerUser() - default address created, shippingId={}", savedAddress.getShippingId());

        return buildResponse(savedUser, savedAddress.getShippingId());
    }

    // ─────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────
    @Override
    public UserResponseDTO getUserById(Long userId) {
        log.info("[UserService] getUserById() - userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[UserService] getUserById() - userId={} not found", userId);
                    return new RuntimeException("User not found: " + userId);
                });

        Long defaultAddressId = shippingAddressRepository
                .findByUserAndIsDefaultTrue(user)
                .map(ShippingAddressEntity::getShippingId)
                .orElse(null);

        OrderStats orderStats = orderRepository.getOrderStatsByUserId(userId); // add this

        log.info("[UserService] getUserById() - found userId={}", userId);
        return buildUserWithOrderResponse(user, defaultAddressId, orderStats); // pass it in
    }

    // ─────────────────────────────────────────────────────────────
    // GET ALL WITH PAGINATION
    // ─────────────────────────────────────────────────────────────
    @Override
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        log.info("[UserService] getAllUsers() - page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<UserEntity> users = userRepository.findAll(pageable);
        log.info("[UserService] getAllUsers() - totalElements={}", users.getTotalElements());

        return users.map(user -> {
            Long defaultAddressId = shippingAddressRepository
                    .findByUserAndIsDefaultTrue(user)
                    .map(ShippingAddressEntity::getShippingId)
                    .orElse(null);
            return buildResponse(user, defaultAddressId);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH USER
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public UserResponseDTO patchUser(Long userId, UserPatchDTO dto) {
        log.info("[UserService] patchUser() - userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[UserService] patchUser() - userId={} not found", userId);
                    return new RuntimeException("User not found: " + userId);
                });

        StringBuilder changes = new StringBuilder();

        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
            changes.append("firstName ");
        }
        if (dto.getMiddleName() != null) {
            user.setMiddleName(dto.getMiddleName());
            changes.append("middleName ");
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
            changes.append("lastName ");
        }
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                log.warn("[UserService] patchUser() - email conflict: {}", dto.getEmail());
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(dto.getEmail());
            changes.append("email ");
        }
        if (dto.getPhone() != null) {
            if (!dto.getPhone().equals(user.getPhone()) && userRepository.existsByPhone(dto.getPhone())) {
                log.warn("[UserService] patchUser() - phone conflict: {}", dto.getPhone());
                throw new RuntimeException("Phone number already in use");
            }
            user.setPhone(dto.getPhone());
            changes.append("phone ");
        }

        UserEntity updated = userRepository.save(user);
        log.info("[UserService] patchUser() - userId={} updated, changed fields: [{}]",
                userId, changes.toString().trim());

        Long defaultAddressId = shippingAddressRepository
                .findByUserAndIsDefaultTrue(updated)
                .map(ShippingAddressEntity::getShippingId)
                .orElse(null);

        return buildResponse(updated, defaultAddressId);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE USER
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("[UserService] deleteUser() - userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[UserService] deleteUser() - userId={} not found", userId);
                    return new RuntimeException("User not found: " + userId);
                });

        // Cascades to shipping addresses via CascadeType.ALL on UserEntity
        userRepository.delete(user);
        log.info("[UserService] deleteUser() - userId={} deleted (addresses cascade-deleted)", userId);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────
    private UserResponseDTO buildResponse(UserEntity user, Long defaultAddressId) {

        UserResponseDTO response = new UserResponseDTO();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setMiddleName(user.getMiddleName());
        response.setLastName(user.getLastName());

        String fullName = (user.getMiddleName() != null && !user.getMiddleName().isBlank())
                ? user.getFirstName() + " " + user.getMiddleName() + " " + user.getLastName()
                : user.getFirstName() + " " + user.getLastName();
        response.setFullName(fullName);

        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        response.setDefaultShippingAddressId(defaultAddressId);
        return response;
    }

    private UserResponseDTO buildUserWithOrderResponse(UserEntity user, Long defaultAddressId, OrderStats orderStats) {

        UserResponseDTO response = new UserResponseDTO();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setMiddleName(user.getMiddleName());
        response.setLastName(user.getLastName());

        String fullName = (user.getMiddleName() != null && !user.getMiddleName().isBlank())
                ? user.getFirstName() + " " + user.getMiddleName() + " " + user.getLastName()
                : user.getFirstName() + " " + user.getLastName();
        response.setFullName(fullName);

        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        response.setDefaultShippingAddressId(defaultAddressId);

        // order stats
        response.setTotalOrdersCount(orderStats.getTotalOrdersCount());
        response.setTotalSpent(orderStats.getTotalSpent());

        return response;
    }
}