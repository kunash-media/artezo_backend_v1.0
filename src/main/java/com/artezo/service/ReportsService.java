package com.artezo.service;

import com.artezo.dto.stats.reports.*;

import java.util.List;

public interface ReportsService {

    /**
     * Full page load — all sections in one call.
     * @param period   "today" | "week" | "month" | "quarter" | "year" | "custom"
     * @param start    ISO date string "yyyy-MM-dd" — required only when period = "custom"
     * @param end      ISO date string "yyyy-MM-dd" — required only when period = "custom"
     * @param granularity "daily" | "weekly" | "monthly"
     */
    ReportsFullResponseDto getFullReport(String period, String start, String end, String granularity);

    /** KPI cards only */
    List<ReportsKpiDto> getKpis(String period, String start, String end);

    /** Sales trend bar chart */
    List<SalesTrendDto> getTrend(String period, String start, String end, String granularity);

    /** Donut chart — revenue by category */
    List<CategoryRevenueDto> getCategories(String period, String start, String end);

    /**
     * Top products table.
     * @param categoryFilter "all" or a category name e.g. "Electronics"
     * @param sortBy         "revenue" | "units" | "growth"
     */
    List<TopProductDto> getTopProducts(String period, String start, String end,
                                       String categoryFilter, String sortBy);

    /** Today's summary panel — always IST CURDATE(), ignores period params */
    TodaySummaryDto getTodaySummary();

    /** Payment methods breakdown */
    List<PaymentMethodDto> getPaymentMethods(String period, String start, String end);

    /** Order status breakdown */
    List<OrderStatusDto> getOrderStatus(String period, String start, String end);
}