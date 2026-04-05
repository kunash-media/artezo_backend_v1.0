package com.artezo.service.serviceImpl;

import com.artezo.dto.stats.reports.*;
import com.artezo.repository.ReportsRepository;
import com.artezo.service.ReportsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All monetary arithmetic uses BigDecimal to prevent lossy double operations.
 * DB stores amounts as Double (rupees, 2 decimal places) — we wrap in BigDecimal
 * for all division/percentage calculations and return Double for JSON serialisation.
 *
 * Currency display format: ₹ — Indian number system (lakh/crore separators)
 * via java.text.NumberFormat with Locale.forLanguageTag("en-IN").
 */
@Service
@Transactional(readOnly = true)
public class ReportsServiceImpl implements ReportsService {

    private static final Logger log = LoggerFactory.getLogger(ReportsServiceImpl.class);

    private final ReportsRepository reportsRepository;

    public ReportsServiceImpl(ReportsRepository reportsRepository) {
        this.reportsRepository = reportsRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FULL REPORT — single call, powers the full page load
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public ReportsFullResponseDto getFullReport(String period, String start, String end,
                                                String granularity) {
        DateRange range = resolveDateRange(period, start, end);
        log.info("Full report requested — period={}, start={}, end={}, granularity={}",
                period, range.start, range.end, granularity);

        ReportsFullResponseDto dto = new ReportsFullResponseDto();
        dto.setPeriod(period);
        dto.setPeriodStart(range.start);
        dto.setPeriodEnd(range.end);
        dto.setKpis(buildKpis(range));
        dto.setTrend(buildTrend(range, granularity));
        dto.setTrendGranularity(granularity != null ? granularity : "daily");
        dto.setCategories(buildCategories(range));
        dto.setTopProducts(buildTopProducts(range, "all", "revenue"));
        dto.setTodaySummary(buildTodaySummary());
        dto.setPaymentMethods(buildPaymentMethods(range));
        dto.setOrderStatus(buildOrderStatus(range));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  KPI CARDS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<ReportsKpiDto> getKpis(String period, String start, String end) {
        return buildKpis(resolveDateRange(period, start, end));
    }

    private List<ReportsKpiDto> buildKpis(DateRange range) {
        DateRange prev = previousRange(range);

        // ── Revenue ──────────────────────────────────────────────────────
        Double curRevRaw  = nullSafeDouble(reportsRepository.getRevenueBetween(range.start, range.end));
        Double prevRevRaw = nullSafeDouble(reportsRepository.getRevenueBetween(prev.start, prev.end));
        double revDelta   = computeDeltaPct(curRevRaw, prevRevRaw);

        // ── Orders ───────────────────────────────────────────────────────
        Long   curOrders  = nullSafeLong(reportsRepository.getOrderCountBetween(range.start, range.end));
        Long   prevOrders = nullSafeLong(reportsRepository.getOrderCountBetween(prev.start, prev.end));
        double ordDelta   = computeDeltaPct(curOrders.doubleValue(), prevOrders.doubleValue());

        // ── Avg Order Value ───────────────────────────────────────────────
        Double curAov  = nullSafeDouble(reportsRepository.getAvgOrderValueBetween(range.start, range.end));
        Double prevAov = nullSafeDouble(reportsRepository.getAvgOrderValueBetween(prev.start, prev.end));
        double aovDelta = computeDeltaPct(curAov, prevAov);

        List<ReportsKpiDto> kpis = new ArrayList<>();

        kpis.add(new ReportsKpiDto(
                "Total Revenue",
                formatRupees(curRevRaw),
                curRevRaw,
                roundTwoDecimal(revDelta),
                revDelta >= 0,
                "fa-indian-rupee-sign"
        ));

        kpis.add(new ReportsKpiDto(
                "Total Orders",
                formatLong(curOrders),
                curOrders.doubleValue(),
                roundTwoDecimal(ordDelta),
                ordDelta >= 0,
                "fa-shopping-bag"
        ));

        kpis.add(new ReportsKpiDto(
                "Avg. Order Value",
                formatRupees(curAov),
                curAov,
                roundTwoDecimal(aovDelta),
                aovDelta >= 0,
                "fa-receipt"
        ));

        log.debug("KPIs built — revenue={}, orders={}, aov={}", curRevRaw, curOrders, curAov);
        return kpis;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SALES TREND
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<SalesTrendDto> getTrend(String period, String start, String end, String granularity) {
        return buildTrend(resolveDateRange(period, start, end), granularity);
    }

    private List<SalesTrendDto> buildTrend(DateRange range, String granularity) {
        String g = (granularity == null || granularity.isBlank()) ? "daily" : granularity;
        List<Object[]> rows;

        switch (g) {
            case "weekly"  -> rows = reportsRepository.getWeeklyTrend(range.start, range.end);
            case "monthly" -> rows = reportsRepository.getMonthlyTrend(range.start, range.end);
            default        -> rows = reportsRepository.getDailyTrend(range.start, range.end);
        }

        List<SalesTrendDto> trend = new ArrayList<>();
        for (Object[] row : rows) {
            String label   = row[0] != null ? row[0].toString() : "";
            Double revenue = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            trend.add(new SalesTrendDto(label, revenue));
        }
        return trend;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REVENUE BY CATEGORY
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<CategoryRevenueDto> getCategories(String period, String start, String end) {
        return buildCategories(resolveDateRange(period, start, end));
    }

    private List<CategoryRevenueDto> buildCategories(DateRange range) {
        List<Object[]> rows = reportsRepository.getCategoryRevenueByPeriod(range.start, range.end);

        // ── compute total using BigDecimal — no lossy double addition ────
        BigDecimal total = BigDecimal.ZERO;
        List<String[]> interim = new ArrayList<>();

        for (Object[] row : rows) {
            String catName = row[0] != null ? row[0].toString() : "Uncategorized";
            BigDecimal rev = row[1] != null
                    ? BigDecimal.valueOf(((Number) row[1]).doubleValue())
                    : BigDecimal.ZERO;
            total = total.add(rev);
            interim.add(new String[]{catName, rev.toPlainString()});
        }

        List<CategoryRevenueDto> result = new ArrayList<>();
        for (String[] entry : interim) {
            BigDecimal rev = new BigDecimal(entry[1]);
            // percentage = (rev / total) * 100  — scale 2, HALF_UP
            double pct = total.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : rev.multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            result.add(new CategoryRevenueDto(entry[0], rev.doubleValue(), pct));
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TOP PRODUCTS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<TopProductDto> getTopProducts(String period, String start, String end,
                                              String categoryFilter, String sortBy) {
        return buildTopProducts(resolveDateRange(period, start, end), categoryFilter, sortBy);
    }

    private List<TopProductDto> buildTopProducts(DateRange range,
                                                 String categoryFilter, String sortBy) {
        List<Object[]> rows = reportsRepository.getTopProductsByPeriod(range.start, range.end);
        DateRange prev = previousRange(range);

        List<TopProductDto> products = new ArrayList<>();

        for (Object[] row : rows) {
            // col order: productStrId, productName, brandName, category, unitsSold, revenue
            String productStrId = row[0] != null ? row[0].toString() : "";
            String productName  = row[1] != null ? row[1].toString() : "";
            String brandName    = row[2] != null ? row[2].toString() : "";
            String category     = row[3] != null ? row[3].toString() : "Uncategorized";
            Long   unitsSold    = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            BigDecimal revenue  = row[5] != null
                    ? BigDecimal.valueOf(((Number) row[5]).doubleValue())
                    : BigDecimal.ZERO;

            // ── category filter ──────────────────────────────────────────
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("all")) {
                if (!category.equalsIgnoreCase(categoryFilter)) continue;
            }

            // ── avg price — BigDecimal division ──────────────────────────
            BigDecimal avgPrice = unitsSold > 0
                    ? revenue.divide(BigDecimal.valueOf(unitsSold), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // ── growth % vs previous period ───────────────────────────────
            Double prevRevRaw = nullSafeDouble(
                    reportsRepository.getProductRevenueBetween(productStrId, prev.start, prev.end)
            );
            BigDecimal prevRev = BigDecimal.valueOf(prevRevRaw);
            double growthPct   = computeDeltaPct(revenue.doubleValue(), prevRev.doubleValue());

            TopProductDto dto = new TopProductDto();
            dto.setProductStrId(productStrId);
            dto.setProductName(productName);
            dto.setBrandName(brandName);
            dto.setCategory(category);
            dto.setUnitsSold(unitsSold);
            dto.setRevenue(revenue.doubleValue());
            dto.setAvgPrice(avgPrice.doubleValue());
            dto.setGrowthPercent(roundTwoDecimal(growthPct));
            products.add(dto);
        }

        // ── sort ─────────────────────────────────────────────────────────
        if (sortBy != null) {
            switch (sortBy) {
                case "units"  -> products.sort(Comparator.comparingLong(TopProductDto::getUnitsSold).reversed());
                case "growth" -> products.sort(Comparator.comparingDouble(
                        p -> (p.getGrowthPercent() != null ? p.getGrowthPercent() : 0.0)));
                default       -> products.sort(Comparator.comparingDouble(TopProductDto::getRevenue).reversed());
            }
            if ("growth".equals(sortBy)) {
                // reversed separately because lambda above can't be chained on Comparator.comparingDouble with reversed
                products.sort(Comparator.comparingDouble(
                        (TopProductDto p) -> p.getGrowthPercent() != null ? p.getGrowthPercent() : 0.0).reversed());
            }
        }

        return products;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TODAY'S SUMMARY
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public TodaySummaryDto getTodaySummary() {
        return buildTodaySummary();
    }

    private TodaySummaryDto buildTodaySummary() {
        TodaySummaryDto dto = new TodaySummaryDto();
        dto.setRevenue(nullSafeDouble(reportsRepository.getTodayRevenue()));
        dto.setOrders(nullSafeLong(reportsRepository.getTodayOrders()));
        dto.setUniqueCustomers(nullSafeLong(reportsRepository.getTodayUniqueCustomers()));
        dto.setAvgOrderValue(nullSafeDouble(reportsRepository.getTodayAvgOrderValue()));
        dto.setRefunds(nullSafeLong(reportsRepository.getTodayRefunds()));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAYMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<PaymentMethodDto> getPaymentMethods(String period, String start, String end) {
        return buildPaymentMethods(resolveDateRange(period, start, end));
    }

    private List<PaymentMethodDto> buildPaymentMethods(DateRange range) {
        List<Object[]> rows = reportsRepository.getPaymentMethodBreakdown(range.start, range.end);

        // total count for percentage — BigDecimal
        BigDecimal total = BigDecimal.ZERO;
        List<Object[]> cached = new ArrayList<>(rows);
        for (Object[] row : cached) {
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            total = total.add(BigDecimal.valueOf(cnt));
        }

        List<PaymentMethodDto> result = new ArrayList<>();
        for (Object[] row : cached) {
            String label = row[0] != null ? row[0].toString() : "Other";
            long   count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            double pct   = total.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : BigDecimal.valueOf(count)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            result.add(new PaymentMethodDto(label, count, pct));
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ORDER STATUS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<OrderStatusDto> getOrderStatus(String period, String start, String end) {
        return buildOrderStatus(resolveDateRange(period, start, end));
    }

    private List<OrderStatusDto> buildOrderStatus(DateRange range) {
        List<Object[]> rows = reportsRepository.getOrderStatusBreakdown(range.start, range.end);

        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : rows) {
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            total = total.add(BigDecimal.valueOf(cnt));
        }

        List<OrderStatusDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String status = row[0] != null ? row[0].toString() : "UNKNOWN";
            long   count  = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            double pct    = total.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : BigDecimal.valueOf(count)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            result.add(new OrderStatusDto(status, count, pct));
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DATE RANGE RESOLUTION
    //  All dates returned as "yyyy-MM-dd" strings — MySQL DATE() compatible
    // ═══════════════════════════════════════════════════════════════════════

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateRange resolveDateRange(String period, String customStart, String customEnd) {
        LocalDate today = LocalDate.now(); // JVM is UTC; CONVERT_TZ handles IST in SQL
        return switch (period == null ? "today" : period.toLowerCase()) {
            case "week"    -> new DateRange(
                    today.with(java.time.DayOfWeek.MONDAY).format(ISO),
                    today.format(ISO));
            case "month"   -> new DateRange(
                    today.withDayOfMonth(1).format(ISO),
                    today.format(ISO));
            case "quarter" -> {
                int q = (today.getMonthValue() - 1) / 3;
                LocalDate qStart = today.withMonth(q * 3 + 1).withDayOfMonth(1);
                yield new DateRange(qStart.format(ISO), today.format(ISO));
            }
            case "year"    -> new DateRange(
                    today.withDayOfYear(1).format(ISO),
                    today.format(ISO));
            case "custom"  -> {
                if (customStart == null || customEnd == null) {
                    yield new DateRange(today.format(ISO), today.format(ISO));
                }
                yield new DateRange(customStart, customEnd);
            }
            default        -> new DateRange(today.format(ISO), today.format(ISO)); // "today"
        };
    }

    /**
     * Returns the previous window of the same length as the current range.
     * Used for delta % KPI calculations.
     */
    private DateRange previousRange(DateRange current) {
        LocalDate start = LocalDate.parse(current.start, ISO);
        LocalDate end   = LocalDate.parse(current.end, ISO);
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevEnd   = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);
        return new DateRange(prevStart.format(ISO), prevEnd.format(ISO));
    }

    private record DateRange(String start, String end) {}

    // ═══════════════════════════════════════════════════════════════════════
    //  ARITHMETIC HELPERS  — all BigDecimal, no double arithmetic
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Delta percentage: ((current - previous) / previous) * 100
     * Returns 0.0 when previous is 0 to avoid division by zero.
     * Result is rounded to 2 decimal places, HALF_UP.
     */
    private double computeDeltaPct(double current, double previous) {
        if (previous == 0.0) return 0.0;
        BigDecimal cur  = BigDecimal.valueOf(current);
        BigDecimal prev = BigDecimal.valueOf(previous);
        return cur.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Round to 2 decimal places HALF_UP — used for delta and growth % */
    private double roundTwoDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FORMATTING HELPERS  — ₹ Indian locale, no rounding of source value
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Formats a rupee amount in Indian number system.
     * e.g. 128430.50 → "₹1,28,430.50"
     * Uses BigDecimal to preserve exact decimal places from DB.
     */
    private String formatRupees(Double amount) {
        if (amount == null) return "₹0.00";
        BigDecimal bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.UNNECESSARY);

        // Indian grouping: last 3 digits, then groups of 2
        java.text.NumberFormat nf =
                java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("en-IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        return "₹" + nf.format(bd);
    }

    private String formatLong(Long value) {
        if (value == null) return "0";
        java.text.NumberFormat nf =
                java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("en-IN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NULL SAFETY — DB COALESCE returns 0 but Spring can still map to null
    // ═══════════════════════════════════════════════════════════════════════

    private Double nullSafeDouble(Double val) { return val != null ? val : 0.0; }
    private Long   nullSafeLong(Long val)     { return val != null ? val : 0L; }
}