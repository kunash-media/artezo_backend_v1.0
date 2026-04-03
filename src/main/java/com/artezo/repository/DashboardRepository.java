package com.artezo.repository;

import com.artezo.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<OrderEntity, Long> {


    @Query(value = "SELECT CURDATE() as curdate, NOW() as now_time, @@session.time_zone as tz", nativeQuery = true)
    List<Object[]> getTimezoneCheck();


    // ── STATS ─────────────────────────────────────────────────────────────────

    @Query(value = """
    SELECT COALESCE(SUM(o.final_amount), 0)
    FROM orders o
    WHERE DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
    """, nativeQuery = true)
    Double getTodaySales();

    @Query(value = """
    SELECT COUNT(o.order_id)
    FROM orders o
    WHERE DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) = CURDATE()
    """, nativeQuery = true)
    Long getTodayOrders();

    @Query(value = """
    SELECT COUNT(o.order_id)
    FROM orders o
    WHERE o.order_status != 'CANCELLED'
    """, nativeQuery = true)
    Long getTotalOrders();

    @Query(value = """
    SELECT COALESCE(SUM(o.final_amount), 0)
    FROM orders o
    WHERE o.payment_status = 'PAID'
    """, nativeQuery = true)
    Double getTotalRevenue();

    @Query(value = """
    SELECT DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) as saleDate,
           COALESCE(SUM(o.final_amount), 0) as totalSales
    FROM orders o
    WHERE CONVERT_TZ(o.order_date, '+00:00', '+05:30') >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
    GROUP BY DATE(CONVERT_TZ(o.order_date, '+00:00', '+05:30'))
    ORDER BY saleDate ASC
    """, nativeQuery = true)
    List<Object[]> getSalesTrend();

    @Query(value = """
    SELECT p.product_category,
           COUNT(oi.order_item_id) as orderCount
    FROM order_items oi
    JOIN products_table p ON oi.product_str_id = p.product_str_id
    GROUP BY p.product_category
    ORDER BY orderCount DESC
    LIMIT 6
    """, nativeQuery = true)
    List<Object[]> getCategorySales();

    @Query(value = """
    SELECT oi.product_str_id,
           oi.product_name,
           SUM(oi.quantity)   as totalSold,
           SUM(oi.item_total) as revenue
    FROM order_items oi
    GROUP BY oi.product_str_id, oi.product_name
    ORDER BY totalSold DESC
    LIMIT 5
    """, nativeQuery = true)
    List<Object[]> getTopProducts();

    @Query(value = """
    SELECT HOUR(CONVERT_TZ(o.order_date, '+00:00', '+05:30')) as hourSlot,
           COUNT(o.order_id) as orderCount
    FROM orders o
    WHERE CONVERT_TZ(o.order_date, '+00:00', '+05:30') >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    GROUP BY HOUR(CONVERT_TZ(o.order_date, '+00:00', '+05:30'))
    ORDER BY hourSlot ASC
    """, nativeQuery = true)
    List<Object[]> getOrderFrequency();

    @Query(value = """
    SELECT o.order_str_id,
           o.customer_name,
           o.final_amount,
           o.order_status,
           o.order_date,
           (SELECT oi.product_name
            FROM order_items oi
            WHERE oi.order_id = o.order_id
            LIMIT 1) as productName
    FROM orders o
    ORDER BY o.order_date DESC
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getRecentOrders();
}