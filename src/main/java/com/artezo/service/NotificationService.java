package com.artezo.service;

import com.artezo.dto.response.NotificationAlertDTO;
import com.artezo.entity.AdminNotificationStateEntity;
import com.artezo.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final AdminNotificationStateRepository stateRepository;

    private static final int RECENT_DAYS = 7;

    public NotificationService(InventoryRepository inventoryRepository,
                               UserRepository userRepository,
                               ContactRepository contactRepository,
                               AdminNotificationStateRepository stateRepository) {
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.stateRepository = stateRepository;
    }

    @Transactional(readOnly = true)
    public NotificationAlertDTO getAlerts(String adminId) {
        // Load all visited fingerprints for this admin in one query
        Set<String> visited = stateRepository.findByAdminId(adminId)
                .stream()
                .map(AdminNotificationStateEntity::getFingerprint)
                .collect(Collectors.toSet());

        List<NotificationAlertDTO.LowStockAlert> lowStock =
                inventoryRepository.findLowStockItems().stream()
                        .map(inv -> new NotificationAlertDTO.LowStockAlert(
                                inv.getSku(),
                                inv.getProduct() != null ? inv.getProduct().getProductName() : inv.getSku(),
                                inv.getAvailableStock(),
                                inv.getLowStockThreshold(),
                                visited.contains("stock-" + inv.getSku())   // ← pass visited flag
                        ))
                        .collect(Collectors.toList());

        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);

        List<NotificationAlertDTO.NewUserAlert> recentUsers =
                userRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since).stream()
                        .map(u -> new NotificationAlertDTO.NewUserAlert(
                                u.getUserId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                                u.getCreatedAt(),
                                visited.contains("user-" + u.getUserId())   // ← pass visited flag
                        ))
                        .collect(Collectors.toList());

        List<NotificationAlertDTO.ContactEnquiryAlert> enquiries =
                contactRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since).stream()
                        .map(c -> new NotificationAlertDTO.ContactEnquiryAlert(
                                c.getFormId(), c.getName(), c.getEmail(), c.getMessage(),
                                c.getCreatedAt(),
                                visited.contains("contact-" + c.getFormId())  // ← pass visited flag
                        ))
                        .collect(Collectors.toList());

        return new NotificationAlertDTO(lowStock, recentUsers, enquiries);
    }

    // ── Mark a single item visited ───────────────────────────────
    @Transactional
    public void markVisited(String adminId, String fingerprint) {
        if (!stateRepository.existsByAdminIdAndFingerprint(adminId, fingerprint)) {
            stateRepository.save(new AdminNotificationStateEntity(adminId, fingerprint));
            log.info("Marked visited → adminId: {}, fingerprint: {}", adminId, fingerprint);
        }
    }

    // ── Mark all current alerts visited at once ──────────────────
    @Transactional
    public void markAllVisited(String adminId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);

        inventoryRepository.findLowStockItems()
                .forEach(inv -> markVisited(adminId, "stock-" + inv.getSku()));

        userRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since)
                .forEach(u -> markVisited(adminId, "user-" + u.getUserId()));

        contactRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since)
                .forEach(c -> markVisited(adminId, "contact-" + c.getFormId()));

        log.info("All notifications marked visited for adminId: {}", adminId);
    }
}