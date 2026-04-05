package com.artezo.dto.stats.reports;

import java.util.List;

public class ReportsFullResponseDto {

    private List<ReportsKpiDto>      kpis;
    private List<SalesTrendDto>      trend;
    private String                   trendGranularity;  // "daily" | "weekly" | "monthly"
    private List<CategoryRevenueDto> categories;
    private List<TopProductDto>      topProducts;
    private TodaySummaryDto          todaySummary;
    private List<PaymentMethodDto>   paymentMethods;
    private List<OrderStatusDto>     orderStatus;
    private String                   period;            // "today" | "week" | "month" …
    private String                   periodStart;       // "2025-04-01"
    private String                   periodEnd;         // "2025-04-30"

    public ReportsFullResponseDto() {}

    public List<ReportsKpiDto>      getKpis()             { return kpis; }
    public List<SalesTrendDto>      getTrend()            { return trend; }
    public String                   getTrendGranularity() { return trendGranularity; }
    public List<CategoryRevenueDto> getCategories()       { return categories; }
    public List<TopProductDto>      getTopProducts()      { return topProducts; }
    public TodaySummaryDto          getTodaySummary()     { return todaySummary; }
    public List<PaymentMethodDto>   getPaymentMethods()   { return paymentMethods; }
    public List<OrderStatusDto>     getOrderStatus()      { return orderStatus; }
    public String                   getPeriod()           { return period; }
    public String                   getPeriodStart()      { return periodStart; }
    public String                   getPeriodEnd()        { return periodEnd; }

    public void setKpis(List<ReportsKpiDto> kpis)                        { this.kpis = kpis; }
    public void setTrend(List<SalesTrendDto> trend)                      { this.trend = trend; }
    public void setTrendGranularity(String trendGranularity)             { this.trendGranularity = trendGranularity; }
    public void setCategories(List<CategoryRevenueDto> categories)       { this.categories = categories; }
    public void setTopProducts(List<TopProductDto> topProducts)          { this.topProducts = topProducts; }
    public void setTodaySummary(TodaySummaryDto todaySummary)            { this.todaySummary = todaySummary; }
    public void setPaymentMethods(List<PaymentMethodDto> paymentMethods) { this.paymentMethods = paymentMethods; }
    public void setOrderStatus(List<OrderStatusDto> orderStatus)         { this.orderStatus = orderStatus; }
    public void setPeriod(String period)                                  { this.period = period; }
    public void setPeriodStart(String periodStart)                        { this.periodStart = periodStart; }
    public void setPeriodEnd(String periodEnd)                            { this.periodEnd = periodEnd; }
}