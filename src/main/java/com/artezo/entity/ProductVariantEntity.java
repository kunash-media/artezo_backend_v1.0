package com.artezo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
public class ProductVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    private String variantId;       // VAR-GOLD, VAR-BLACK
    private String titleName;       // "matte, glossy etc"
    private String color;
    private String sku;
    private Double price;
    private Double mrp;
    private Integer stock;

    @Lob
    private byte[] mainImageData;
    private String mainImageStrUrl;

    @ElementCollection
    @CollectionTable(name = "variant_mockup_images", joinColumns = @JoinColumn(name = "variant_id"))
    @Lob
    private List<byte[]> mockupImageDataList = new ArrayList<>();

    private LocalDate mfgDate;
    private LocalDate expDate;
    private String size;

    @Column(columnDefinition = "JSON")
    private String couponAvailable;     // JSON array


    public ProductVariantEntity(){}


    public ProductVariantEntity(Long id, ProductEntity product, String variantId, String titleName,
                                String color, String sku, Double price, Double mrp, Integer stock,
                                byte[] mainImageData, String mainImageStrUrl, List<byte[]> mockupImageDataList,
                                LocalDate mfgDate, LocalDate expDate, String size, String couponAvailable) {
        this.id = id;
        this.product = product;
        this.variantId = variantId;
        this.titleName = titleName;
        this.color = color;
        this.sku = sku;
        this.price = price;
        this.mrp = mrp;
        this.stock = stock;
        this.mainImageData = mainImageData;
        this.mainImageStrUrl = mainImageStrUrl;
        this.mockupImageDataList = mockupImageDataList;
        this.mfgDate = mfgDate;
        this.expDate = expDate;
        this.size = size;
        this.couponAvailable = couponAvailable;
    }

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

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getMrp() {
        return mrp;
    }

    public void setMrp(Double mrp) {
        this.mrp = mrp;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public byte[] getMainImageData() {
        return mainImageData;
    }

    public void setMainImageData(byte[] mainImageData) {
        this.mainImageData = mainImageData;
    }

    public String getMainImageStrUrl() {
        return mainImageStrUrl;
    }

    public void setMainImageStrUrl(String mainImageStrUrl) {
        this.mainImageStrUrl = mainImageStrUrl;
    }

    public List<byte[]> getMockupImageDataList() {
        return mockupImageDataList;
    }

    public void setMockupImageDataList(List<byte[]> mockupImageDataList) {
        this.mockupImageDataList = mockupImageDataList;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCouponAvailable() {
        return couponAvailable;
    }

    public void setCouponAvailable(String couponAvailable) {
        this.couponAvailable = couponAvailable;
    }
}