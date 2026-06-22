package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks every user-uploaded customization image.
 * Lifecycle: PENDING → IN_CART → ORDERED → FULFILLED
 *            PENDING → EXPIRED  (GC job after 7 days if never carted)
 */
@Entity
@Table(name = "customization_asset")
public class CustomizationAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID sent to frontend — used as reference in all API calls
    @Column(name = "asset_uuid", nullable = false, unique = true, length = 36)
    private String assetUuid;

    // nullable — guest users won't have userId
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    // What the user originally uploaded
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    // Actual file stored on disk: {assetUuid}_{originalFilename}
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    // Full path: /customized_img/{storedFilename}
    // On VPS this will be reconfigured to absolute path
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssetStatus status = AssetStatus.PENDING;

    // PENDING assets older than this are deleted by GC scheduler
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Lifecycle enum ────────────────────────────────────────────────────────
    public enum AssetStatus {
        PENDING,    // uploaded, not yet in any cart
        IN_CART,    // linked to a CartItem
        ORDERED,    // linked to an OrderItem — must NOT be GC'd
        FULFILLED,  // warehouse printed/used it
        EXPIRED     // soft-marked before physical deletion
    }

    public CustomizationAssetEntity() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getAssetUuid() { return assetUuid; }
    public void setAssetUuid(String assetUuid) { this.assetUuid = assetUuid; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}