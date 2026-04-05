package com.artezo.dto.stats.reports;


public class TopProductDto {

    private String productStrId;   // PRD0001
    private String productName;    // snapshot name from order_items
    private String brandName;
    private String category;
    private Long   unitsSold;
    private Double revenue;        // rupees — exact sum, no rounding
    private Double avgPrice;       // revenue / unitsSold — BigDecimal division in service
    private Double growthPercent;  // +15.0 or -2.5 vs previous period (null if no prev data)

    public TopProductDto() {}

    public String getProductStrId()  { return productStrId; }
    public String getProductName()   { return productName; }
    public String getBrandName()     { return brandName; }
    public String getCategory()      { return category; }
    public Long   getUnitsSold()     { return unitsSold; }
    public Double getRevenue()       { return revenue; }
    public Double getAvgPrice()      { return avgPrice; }
    public Double getGrowthPercent() { return growthPercent; }

    public void setProductStrId(String productStrId)   { this.productStrId = productStrId; }
    public void setProductName(String productName)     { this.productName = productName; }
    public void setBrandName(String brandName)         { this.brandName = brandName; }
    public void setCategory(String category)           { this.category = category; }
    public void setUnitsSold(Long unitsSold)           { this.unitsSold = unitsSold; }
    public void setRevenue(Double revenue)             { this.revenue = revenue; }
    public void setAvgPrice(Double avgPrice)           { this.avgPrice = avgPrice; }
    public void setGrowthPercent(Double growthPercent) { this.growthPercent = growthPercent; }
}