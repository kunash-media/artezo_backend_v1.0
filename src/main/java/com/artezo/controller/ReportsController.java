package com.artezo.controller;

import com.artezo.dto.stats.reports.*;
import com.artezo.exceptions.ApiResponse;
import com.artezo.service.ReportsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reports & Sales analytics endpoints.
 * All routes are under /api/admin/reports and require SUPER_ADMIN role.
 *
 * Period values accepted by all period-aware endpoints:
 *   today | week | month | quarter | year | custom
 *
 * When period = "custom", pass start and end as query params (yyyy-MM-dd).
 *
 * Granularity values (trend endpoint only):
 *   daily | weekly | monthly
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ReportsController {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/full
    //  Single call — all sections for the full page load.
    //  Frontend calls this on DOMContentLoaded / period change.
    //
    //  Query params:
    //    period      (required) today | week | month | quarter | year | custom
    //    start       (optional) yyyy-MM-dd — only used when period=custom
    //    end         (optional) yyyy-MM-dd — only used when period=custom
    //    granularity (optional, default=daily) daily | weekly | monthly
    //
    //  Example:
    //    GET /api/admin/reports/full?period=month&granularity=daily
    //    GET /api/admin/reports/full?period=custom&start=2025-01-01&end=2025-03-31&granularity=monthly
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/full")
    public ResponseEntity<ApiResponse<ReportsFullResponseDto>> getFullReport(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end,
            @RequestParam(defaultValue = "daily") String granularity) {

        log.info("Full report request — period={}, granularity={}", period, granularity);
        ReportsFullResponseDto data = reportsService.getFullReport(period, start, end, granularity);
        return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/kpis
    //  KPI cards only — Total Revenue, Total Orders, Avg. Order Value
    //  (Conversion Rate excluded per product decision)
    //
    //  Example:
    //    GET /api/admin/reports/kpis?period=week
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<List<ReportsKpiDto>>> getKpis(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end) {

        log.info("KPIs request — period={}", period);
        List<ReportsKpiDto> data = reportsService.getKpis(period, start, end);
        return ResponseEntity.ok(ApiResponse.success("KPIs fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/trend
    //  Sales trend bar chart data.
    //
    //  Example:
    //    GET /api/admin/reports/trend?period=month&granularity=daily
    //    GET /api/admin/reports/trend?period=year&granularity=monthly
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<SalesTrendDto>>> getTrend(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end,
            @RequestParam(defaultValue = "daily") String granularity) {

        log.info("Trend request — period={}, granularity={}", period, granularity);
        List<SalesTrendDto> data = reportsService.getTrend(period, start, end, granularity);
        return ResponseEntity.ok(ApiResponse.success("Sales trend fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/categories
    //  Revenue by category — powers the donut chart.
    //
    //  Example:
    //    GET /api/admin/reports/categories?period=month
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryRevenueDto>>> getCategories(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end) {

        log.info("Categories request — period={}", period);
        List<CategoryRevenueDto> data = reportsService.getCategories(period, start, end);
        return ResponseEntity.ok(ApiResponse.success("Category revenue fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/products
    //  Top selling products table.
    //
    //  Query params:
    //    category  (optional, default=all)     all | <exact category name>
    //    sort      (optional, default=revenue) revenue | units | growth
    //
    //  Example:
    //    GET /api/admin/reports/products?period=month&category=all&sort=revenue
    //    GET /api/admin/reports/products?period=week&category=Electronics&sort=units
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<TopProductDto>>> getTopProducts(
            @RequestParam(defaultValue = "today")   String period,
            @RequestParam(required = false)         String start,
            @RequestParam(required = false)         String end,
            @RequestParam(defaultValue = "all")     String category,
            @RequestParam(defaultValue = "revenue") String sort) {

        log.info("Top products request — period={}, category={}, sort={}", period, category, sort);
        List<TopProductDto> data = reportsService.getTopProducts(period, start, end, category, sort);
        return ResponseEntity.ok(ApiResponse.success("Top products fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/today
    //  Today's summary panel — always reflects IST CURDATE().
    //  Ignores period params — always today.
    //
    //  Example:
    //    GET /api/admin/reports/today
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodaySummaryDto>> getTodaySummary() {

        log.info("Today's summary request");
        TodaySummaryDto data = reportsService.getTodaySummary();
        return ResponseEntity.ok(ApiResponse.success("Today's summary fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/payments
    //  Payment method breakdown panel.
    //
    //  Example:
    //    GET /api/admin/reports/payments?period=month
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentMethodDto>>> getPaymentMethods(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end) {

        log.info("Payment methods request — period={}", period);
        List<PaymentMethodDto> data = reportsService.getPaymentMethods(period, start, end);
        return ResponseEntity.ok(ApiResponse.success("Payment methods fetched successfully", data));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/admin/reports/order-status
    //  Order status breakdown panel.
    //
    //  Example:
    //    GET /api/admin/reports/order-status?period=month
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/order-status")
    public ResponseEntity<ApiResponse<List<OrderStatusDto>>> getOrderStatus(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false)       String start,
            @RequestParam(required = false)       String end) {

        log.info("Order status request — period={}", period);
        List<OrderStatusDto> data = reportsService.getOrderStatus(period, start, end);
        return ResponseEntity.ok(ApiResponse.success("Order status fetched successfully", data));
    }
}