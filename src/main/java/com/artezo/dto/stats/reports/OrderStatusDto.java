package com.artezo.dto.stats.reports;


public class OrderStatusDto {

    private String status;      // "DELIVERED", "SHIPPED", "PROCESSING", "CANCELLED" …
    private Long   count;
    private Double percentage;  // 0.00–100.00 — computed with BigDecimal in service

    public OrderStatusDto() {}

    public OrderStatusDto(String status, Long count, Double percentage) {
        this.status     = status;
        this.count      = count;
        this.percentage = percentage;
    }

    public String getStatus()     { return status; }
    public Long   getCount()      { return count; }
    public Double getPercentage() { return percentage; }

    public void setStatus(String status)           { this.status = status; }
    public void setCount(Long count)               { this.count = count; }
    public void setPercentage(Double percentage)   { this.percentage = percentage; }
}