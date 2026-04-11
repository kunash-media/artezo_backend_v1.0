package com.artezo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "variant_mockup_images")
public class VariantMockupImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_pk_id", nullable = false)
    private ProductVariantEntity variant;

    @Lob
    @Column(name = "mockup_image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    public VariantMockupImageEntity() {}

    public VariantMockupImageEntity(ProductVariantEntity variant, byte[] imageData) {
        this.variant = variant;
        this.imageData = imageData;
    }

    // getters + setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductVariantEntity getVariant() { return variant; }
    public void setVariant(ProductVariantEntity variant) { this.variant = variant; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}