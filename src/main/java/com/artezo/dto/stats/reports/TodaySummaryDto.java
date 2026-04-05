package com.artezo.dto.stats.reports;

public class TodaySummaryDto {

    private Double revenue;          // rupees — exact from DB, no rounding
    private Long   orders;
    private Long   uniqueCustomers;
    private Double avgOrderValue;    // rupees — exact from DB AVG
    private Long   refunds;

    public TodaySummaryDto() {}

    public Double getRevenue()         { return revenue; }
    public Long   getOrders()          { return orders; }
    public Long   getUniqueCustomers() { return uniqueCustomers; }
    public Double getAvgOrderValue()   { return avgOrderValue; }
    public Long   getRefunds()         { return refunds; }

    public void setRevenue(Double revenue)               { this.revenue = revenue; }
    public void setOrders(Long orders)                   { this.orders = orders; }
    public void setUniqueCustomers(Long uniqueCustomers) { this.uniqueCustomers = uniqueCustomers; }
    public void setAvgOrderValue(Double avgOrderValue)   { this.avgOrderValue = avgOrderValue; }
    public void setRefunds(Long refunds)                 { this.refunds = refunds; }
}