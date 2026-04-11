package com.artezo.dto.request;

import java.time.LocalDate;
import java.util.List;


public class VariantRequestDto {

    private String variantId;
    private String titleName;
    private String color;
    private String sku;
    private Double price;
    private Double mrp;
    private Integer stock;
    private byte[] mainImage;

    private Integer mockupImageCount;
    private List<byte[]> mockupImages;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private String size;
    private List<String> couponAvailable;

    private Double weight;
    private Double length;
    private Double breadth;
    private Double height;

    private Boolean clearMockupImages = false;

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

    public byte[] getMainImage() {
        return mainImage;
    }

    public void setMainImage(byte[] mainImage) {
        this.mainImage = mainImage;
    }

    public List<byte[]> getMockupImages() {
        return mockupImages;
    }

    public void setMockupImages(List<byte[]> mockupImages) {
        this.mockupImages = mockupImages;
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

    public List<String> getCouponAvailable() {
        return couponAvailable;
    }

    public void setCouponAvailable(List<String> couponAvailable) {
        this.couponAvailable = couponAvailable;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getBreadth() {
        return breadth;
    }

    public void setBreadth(Double breadth) {
        this.breadth = breadth;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Integer getMockupImageCount() {
        return mockupImageCount;
    }

    public void setMockupImageCount(Integer mockupImageCount) {
        this.mockupImageCount = mockupImageCount;
    }

    public Boolean getClearMockupImages() {
        return clearMockupImages;
    }

    public void setClearMockupImages(Boolean clearMockupImages) {
        this.clearMockupImages = clearMockupImages;
    }
}
