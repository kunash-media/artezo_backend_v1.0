package com.artezo.controller;

import com.artezo.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {



    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/timezone-check")
    public ResponseEntity<?> timezoneCheck() {
        return ResponseEntity.ok(service.getTimezoneCheck());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/sales-trend")
    public ResponseEntity<?> getSalesTrend() {
        return ResponseEntity.ok(service.getSalesTrend());
    }

    @GetMapping("/category-sales")
    public ResponseEntity<?> getCategorySales() {
        return ResponseEntity.ok(service.getCategorySales());
    }

    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts() {
        return ResponseEntity.ok(service.getTopProducts());
    }

    @GetMapping("/order-frequency")
    public ResponseEntity<?> getOrderFrequency() {
        return ResponseEntity.ok(service.getOrderFrequency());
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders() {
        return ResponseEntity.ok(service.getRecentOrders());
    }
}