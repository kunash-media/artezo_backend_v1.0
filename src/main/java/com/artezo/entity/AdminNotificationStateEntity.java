package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"admin_id", "fingerprint"}))
public class AdminNotificationStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    // e.g. "stock-ART-WPLATE-BLK", "user-16", "contact-2"
    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "visited_at")
    private LocalDateTime visitedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AdminNotificationStateEntity() {}

    public AdminNotificationStateEntity(String adminId, String fingerprint) {
        this.adminId = adminId;
        this.fingerprint = fingerprint;
        this.visitedAt = LocalDateTime.now();
    }

    public String getAdminId() { return adminId; }
    public String getFingerprint() { return fingerprint; }
    public LocalDateTime getVisitedAt() { return visitedAt; }
}