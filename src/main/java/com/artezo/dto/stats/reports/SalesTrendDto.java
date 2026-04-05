package com.artezo.dto.stats.reports;


public class SalesTrendDto {

    private String label;    // "01 Apr", "Wk 14", "Apr 2025"
    private Double revenue;  // rupees — exact Double from DB SUM, no rounding

    public SalesTrendDto() {}

    public SalesTrendDto(String label, Double revenue) {
        this.label   = label;
        this.revenue = revenue;
    }

    public String getLabel()   { return label; }
    public Double getRevenue() { return revenue; }

    public void setLabel(String label)     { this.label = label; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }
}