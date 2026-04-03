package com.artezo.service;

import com.artezo.repository.DashboardRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final DashboardRepository repo;

    public DashboardService(DashboardRepository repo) {
        this.repo = repo;
    }

    public Object getTimezoneCheck() {
        List<Object[]> rows = repo.getTimezoneCheck();
        if (rows.isEmpty()) return "No result";
        Object[] row = rows.get(0);
        return Map.of(
                "curdate", String.valueOf(row[0]),
                "now",     String.valueOf(row[1]),
                "tz",      String.valueOf(row[2])
        );
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todaySales",   repo.getTodaySales());
        stats.put("todayOrders",  repo.getTodayOrders());
        stats.put("totalOrders",  repo.getTotalOrders());
        stats.put("totalRevenue", repo.getTotalRevenue());
        return stats;
    }

    public Map<String, Object> getSalesTrend() {
        List<Object[]> rows = repo.getSalesTrend();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0].toString());                          // date string
            values.add(((Number) row[1]).doubleValue());
        }
        return Map.of("labels", labels, "values", values);
    }

    public List<Map<String, Object>> getCategorySales() {
        List<Object[]> rows = repo.getCategorySales();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return rows.stream().map(row -> {
            long count = ((Number) row[1]).longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category",   row[0]);
            m.put("orderCount", count);
            m.put("percentage", total > 0 ? Math.round((count * 100.0) / total) : 0);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTopProducts() {
        return repo.getTopProducts().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productStrId",  row[0]);
            m.put("productName",   row[1]);
            m.put("totalSold",     ((Number) row[2]).longValue());
            m.put("revenue",       ((Number) row[3]).doubleValue());
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getOrderFrequency() {
        // Bucket 24 hours into 6 readable slots
        int[] buckets    = new int[6];
        String[] labels  = {"00:00","04:00","08:00","12:00","16:00","20:00"};

        for (Object[] row : repo.getOrderFrequency()) {
            int hour  = ((Number) row[0]).intValue();
            int idx   = Math.min(hour / 4, 5);           // 0–3 → 0, 4–7 → 1 ...
            buckets[idx] += ((Number) row[1]).intValue();
        }

        return Map.of(
                "labels", Arrays.asList(labels),
                "values", Arrays.stream(buckets).boxed().collect(Collectors.toList())
        );
    }

    public List<Map<String, Object>> getRecentOrders() {
        return repo.getRecentOrders().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderStrId",    row[0]);
            m.put("customerName",  row[1]);
            m.put("finalAmount",   row[2] != null ? ((Number) row[2]).doubleValue() : 0);
            m.put("orderStatus",   row[3]);
            m.put("orderDate",     row[4]);
            m.put("productName",   row[5]);
            return m;
        }).collect(Collectors.toList());
    }
}