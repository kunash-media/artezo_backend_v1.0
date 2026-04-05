package com.artezo.dto.stats.reports;


public class PaymentMethodDto {

    private String label;       // "UPI", "Cash on Delivery", "Card", "Net Banking", "Razorpay"
    private Long   count;
    private Double percentage;  // 0.00–100.00 — computed with BigDecimal in service

    public PaymentMethodDto() {}

    public PaymentMethodDto(String label, Long count, Double percentage) {
        this.label      = label;
        this.count      = count;
        this.percentage = percentage;
    }

    public String getLabel()      { return label; }
    public Long   getCount()      { return count; }
    public Double getPercentage() { return percentage; }

    public void setLabel(String label)           { this.label = label; }
    public void setCount(Long count)             { this.count = count; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
}