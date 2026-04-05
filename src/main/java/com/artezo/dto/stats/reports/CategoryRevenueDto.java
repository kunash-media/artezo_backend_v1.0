package com.artezo.dto.stats.reports;

public class CategoryRevenueDto {

    private String categoryName;  // "Electronics", "Fashion" …
    private Double revenue;       // rupees — exact Double from DB SUM
    private Double percentage;    // 0.00–100.00 — computed with BigDecimal in service

    public CategoryRevenueDto() {}

    public CategoryRevenueDto(String categoryName, Double revenue, Double percentage) {
        this.categoryName = categoryName;
        this.revenue      = revenue;
        this.percentage   = percentage;
    }

    public String getCategoryName() { return categoryName; }
    public Double getRevenue()      { return revenue; }
    public Double getPercentage()   { return percentage; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setRevenue(Double revenue)           { this.revenue = revenue; }
    public void setPercentage(Double percentage)     { this.percentage = percentage; }
}