package com.artezo.controller;

import com.artezo.dto.request.BuyNowConfirmRequest;
import com.artezo.dto.request.CreateOrderRequest;
import com.artezo.dto.response.OrderResponse;
import com.artezo.dto.response.OrderSummaryResponse;
import com.artezo.exceptions.ApiResponse;
import com.artezo.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── POST /api/orders/create ───────────────────────────────────────────
    // Cart flow — save to DB + Shiprocket simultaneously
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", response));
    }

    // ── POST /api/orders/confirm-buynow ───────────────────────────────────
    // Buy Now flow — SR Checkout handled payment, just store reference in DB
    @PostMapping("/confirm-buynow")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmBuyNow(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody BuyNowConfirmRequest request) {

        OrderResponse response = orderService.confirmBuyNowOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed successfully", response));
    }

    // ── PUT /api/orders/{orderStrId}/cancel ───────────────────────────────
    @PutMapping("/{orderStrId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderStrId) {

        OrderResponse response = orderService.cancelOrder(userId, orderStrId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    // ── PUT /api/orders/{orderStrId}/return ───────────────────────────────
    @PutMapping("/{orderStrId}/return")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderStrId,
            @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.requestReturn(userId, orderStrId, reason);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted successfully", response));
    }

    // ── PUT /api/orders/{orderStrId}/exchange ─────────────────────────────
    @PutMapping("/{orderStrId}/exchange")
    public ResponseEntity<ApiResponse<OrderResponse>> requestExchange(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderStrId,
            @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.requestExchange(userId, orderStrId, reason);
        return ResponseEntity.ok(ApiResponse.success("Exchange request submitted successfully", response));
    }

    // ── GET /api/orders/{orderStrId}/track ────────────────────────────────
    @GetMapping("/{orderStrId}/track")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trackOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderStrId) {

        Map<String, Object> tracking = orderService.trackOrder(userId, orderStrId);
        return ResponseEntity.ok(ApiResponse.success("Tracking details fetched", tracking));
    }

    // ── GET /api/orders/get-by-orderStrId/{orderStrId} ────────────────────
    @GetMapping("/get-by-orderStrId/{orderStrId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderStrId) {

        OrderResponse response = orderService.getOrderById(userId, orderStrId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", response));
    }

    // ── GET /api/orders/get-all-orders?page=0&size=10 ─────────────────────
    // User facing — returns only logged-in user's orders (filtered by X-User-Id)
    // JWT replacement with userId
    @GetMapping("/user/user-all-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderResponse> orders = orderService.getOrdersByUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    //=============================================================
    //              Admin Get all
    //==========================================================
    // ── GET /api/admin/orders?page=0&size=10 ──────────────────────────────
    // Admin orders table — ALL orders across ALL users, newest first
    @GetMapping("/admin/get-all-orders")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderSummaryResponse> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success("All orders fetched successfully", orders));
    }

    //===================================================
    //          get all orders by userId
    //==================================================

    // ── GET /api/admin/orders/user/{userId}?page=0&size=10 ────────────────
    // Admin customer detail — ALL orders of ONE specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderResponse> orders = orderService.getOrdersByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("User orders fetched successfully", orders));
    }


    @GetMapping("/get-by-orderId/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByOrderStrId(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderByOrderId(orderId));
    }
}