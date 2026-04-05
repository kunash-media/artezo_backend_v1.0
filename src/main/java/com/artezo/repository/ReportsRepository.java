package com.artezo.repository;

import com.artezo.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportsRepository extends JpaRepository<OrderEntity, Long> {

    // ═══════════════════════════════════════════════════════════════════════
    //  KPI — TOTAL REVENUE (PAID orders only, period-aware)
    //  Returns: [[totalRevenue]]  — Double (rupees, 2 decimal DB stored)
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT COALESCE(SUM(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Double getRevenueByPeriod(@Param("startDate") String startDate,
                              @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  KPI — TOTAL ORDERS (excludes CANCELLED, period-aware)
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT COUNT(o.order_id)
        FROM orders o
        WHERE o.order_status != 'CANCELLED'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Long getTotalOrdersByPeriod(@Param("startDate") String startDate,
                                @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  KPI — AVERAGE ORDER VALUE (PAID orders only, period-aware)
    //  Returns: Double — rupees
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT COALESCE(AVG(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Double getAvgOrderValueByPeriod(@Param("startDate") String startDate,
                                    @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  KPI DELTA — Revenue for PREVIOUS period (for % change calculation)
    //  Used by service to compute delta between current vs previous window
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT COALESCE(SUM(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Double getRevenueBetween(@Param("startDate") String startDate,
                             @Param("endDate")   String endDate);

    @Query(value = """
        SELECT COUNT(o.order_id)
        FROM orders o
        WHERE o.order_status != 'CANCELLED'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Long getOrderCountBetween(@Param("startDate") String startDate,
                              @Param("endDate")   String endDate);

    @Query(value = """
        SELECT COALESCE(AVG(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Double getAvgOrderValueBetween(@Param("startDate") String startDate,
                                   @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  SALES TREND — DAILY
    //  Returns: [[dateLabel, totalRevenue]] — grouped by IST calendar day
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%d %b') AS label,
            COALESCE(SUM(o.final_amount), 0)                                    AS revenue
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')),
                 DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%d %b')
        ORDER BY DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) ASC
        """, nativeQuery = true)
    List<Object[]> getDailyTrend(@Param("startDate") String startDate,
                                 @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  SALES TREND — WEEKLY
    //  Returns: [[weekLabel, totalRevenue]] — grouped by ISO week
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            CONCAT('Wk ', WEEK(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), 1)) AS label,
            COALESCE(SUM(o.final_amount), 0)                                       AS revenue
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY WEEK(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), 1),
                 YEAR(CONVERT_TZ(o.order_date, '+00:00', '+05:30'))
        ORDER BY YEAR(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) ASC,
                 WEEK(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), 1) ASC
        """, nativeQuery = true)
    List<Object[]> getWeeklyTrend(@Param("startDate") String startDate,
                                  @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  SALES TREND — MONTHLY
    //  Returns: [[monthLabel, totalRevenue]]
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%b %Y') AS label,
            COALESCE(SUM(o.final_amount), 0)                                    AS revenue
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%Y-%m'),
                 DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%b %Y')
        ORDER BY DATE_FORMAT(CONVERT_TZ(o.order_date, '+00:00', '+05:30'), '%Y-%m') ASC
        """, nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("startDate") String startDate,
                                   @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  REVENUE BY CATEGORY
    //  Joins order_items → products_table on product_str_id (snapshot ref)
    //  Returns: [[categoryName, totalRevenue]]
    //  Percentage is computed in service using BigDecimal to avoid lossy division
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            COALESCE(p.product_category, 'Uncategorized') AS categoryName,
            COALESCE(SUM(oi.item_total), 0)               AS revenue
        FROM order_items oi
        JOIN orders o          ON oi.order_id     = o.order_id
        JOIN products_table p  ON oi.product_str_id = p.product_str_id
        WHERE o.payment_status = 'PAID'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY COALESCE(p.product_category, 'Uncategorized')
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryRevenueByPeriod(@Param("startDate") String startDate,
                                              @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  TOP SELLING PRODUCTS
    //  Uses order_items snapshot (product_name, item_total) — NOT products_table
    //  so historical orders reflect the price at time of sale.
    //  Growth: compares current-period revenue vs previous-period revenue.
    //  Returns: [[productStrId, productName, unitsSold, revenue, brandName, category]]
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            oi.product_str_id                              AS productStrId,
            oi.product_name                                AS productName,
            COALESCE(p.brand_name, oi.brand_name, '')     AS brandName,
            COALESCE(p.product_category, 'Uncategorized') AS category,
            SUM(oi.quantity)                               AS unitsSold,
            COALESCE(SUM(oi.item_total), 0)               AS revenue
        FROM order_items oi
        JOIN orders o          ON oi.order_id      = o.order_id
        LEFT JOIN products_table p ON oi.product_str_id = p.product_str_id
        WHERE o.payment_status = 'PAID'
          AND o.order_status   != 'CANCELLED'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY oi.product_str_id, oi.product_name,
                 COALESCE(p.brand_name, oi.brand_name, ''),
                 COALESCE(p.product_category, 'Uncategorized')
        ORDER BY revenue DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getTopProductsByPeriod(@Param("startDate") String startDate,
                                          @Param("endDate")   String endDate);

    // Previous period revenue for a specific product — used for growth % calculation
    @Query(value = """
        SELECT COALESCE(SUM(oi.item_total), 0)
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.order_id
        WHERE o.payment_status   = 'PAID'
          AND o.order_status    != 'CANCELLED'
          AND oi.product_str_id  = :productStrId
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        """, nativeQuery = true)
    Double getProductRevenueBetween(@Param("productStrId") String productStrId,
                                    @Param("startDate")    String startDate,
                                    @Param("endDate")      String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  TODAY'S SUMMARY  (IST calendar day)
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT COALESCE(SUM(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
        """, nativeQuery = true)
    Double getTodayRevenue();

    @Query(value = """
        SELECT COUNT(o.order_id)
        FROM orders o
        WHERE DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
          AND o.order_status != 'CANCELLED'
        """, nativeQuery = true)
    Long getTodayOrders();

    @Query(value = """
        SELECT COUNT(DISTINCT o.user_id)
        FROM orders o
        WHERE DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
          AND o.order_status != 'CANCELLED'
        """, nativeQuery = true)
    Long getTodayUniqueCustomers();

    @Query(value = """
        SELECT COALESCE(AVG(o.final_amount), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
        """, nativeQuery = true)
    Double getTodayAvgOrderValue();

    @Query(value = """
        SELECT COUNT(o.order_id)
        FROM orders o
        WHERE o.payment_status = 'REFUNDED'
          AND DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
        """, nativeQuery = true)
    Long getTodayRefunds();

    // ═══════════════════════════════════════════════════════════════════════
    //  PAYMENT METHODS BREAKDOWN  (period-aware)
    //  Returns: [[paymentMode, orderCount]]
    //  COD → PaymentMethod = COD; all others derive from PaymentMode
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            CASE
                WHEN o.payment_method = 'COD'  THEN 'Cash on Delivery'
                WHEN o.payment_mode   = 'UPI'  THEN 'UPI'
                WHEN o.payment_mode   = 'CARD' THEN 'Card'
                WHEN o.payment_mode   = 'NETBANKING' THEN 'Net Banking'
                WHEN o.payment_mode   = 'RAZORPAY'   THEN 'Razorpay'
                ELSE 'Other'
            END AS paymentLabel,
            COUNT(o.order_id) AS orderCount
        FROM orders o
        WHERE o.order_status != 'CANCELLED'
          AND (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY paymentLabel
        ORDER BY orderCount DESC
        """, nativeQuery = true)
    List<Object[]> getPaymentMethodBreakdown(@Param("startDate") String startDate,
                                             @Param("endDate")   String endDate);

    // ═══════════════════════════════════════════════════════════════════════
    //  ORDER STATUS BREAKDOWN  (period-aware)
    //  Returns: [[orderStatus, count]]
    //  Percentage is computed in service with BigDecimal
    // ═══════════════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT
            o.order_status   AS orderStatus,
            COUNT(o.order_id) AS orderCount
        FROM orders o
        WHERE (:startDate IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) >= :startDate)
          AND (:endDate   IS NULL OR DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) <= :endDate)
        GROUP BY o.order_status
        ORDER BY orderCount DESC
        """, nativeQuery = true)
    List<Object[]> getOrderStatusBreakdown(@Param("startDate") String startDate,
                                           @Param("endDate")   String endDate);
}