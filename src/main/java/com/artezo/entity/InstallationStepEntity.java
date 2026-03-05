package com.artezo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "installation_steps")
@Data
public class InstallationStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Connection to ProductEntity (unidirectional - NO change in ProductEntity)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    private int step;                    // 1, 2, 3 ...

    private String title;
    private String shortDescription;
    private String shortNote;

    @Lob
    private byte[] stepImageData;        // image for this step

    @Lob
    @Column(name = "video_data", columnDefinition = "LONGBLOB")
    private byte[] videoData;            // video file (can be null)

    private String stepImageStrUrl;         // will be generated like /api/products/{productId}/step/{step}/image
    private String videoStrUrl;             // will be generated like /api/products/{productId}/installation-video/{step}

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Default constructor (required by JPA)
    public InstallationStepEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getShortNote() {
        return shortNote;
    }

    public void setShortNote(String shortNote) {
        this.shortNote = shortNote;
    }

    public byte[] getStepImageData() {
        return stepImageData;
    }

    public void setStepImageData(byte[] stepImageData) {
        this.stepImageData = stepImageData;
    }

    public byte[] getVideoData() {
        return videoData;
    }

    public void setVideoData(byte[] videoData) {
        this.videoData = videoData;
    }

    public String getStepImageStrUrl() {
        return stepImageStrUrl;
    }

    public void setStepImageStrUrl(String stepImageStrUrl) {
        this.stepImageStrUrl = stepImageStrUrl;
    }

    public String getVideoStrUrl() {
        return videoStrUrl;
    }

    public void setVideoStrUrl(String videoStrUrl) {
        this.videoStrUrl = videoStrUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}