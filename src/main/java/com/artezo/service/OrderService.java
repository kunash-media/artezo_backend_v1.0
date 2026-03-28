package com.artezo.service;

import com.artezo.dto.request.BuyNowConfirmRequest;
import com.artezo.dto.request.CreateOrderRequest;
import com.artezo.dto.response.OrderResponse;
import com.artezo.dto.response.OrderSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface OrderService {

    /**
     * Cart flow — validates stock, saves as PENDING,
     * calls Shiprocket, updates to CONFIRMED on success.
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * Buy Now flow — SR Checkout already handled payment + shipment.
     * Just stores reference in DB as CONFIRMED + PAID.
     */
    OrderResponse confirmBuyNowOrder(Long userId, BuyNowConfirmRequest request);

    /**
     * Cancels order in DB + Shiprocket.
     * Not allowed if DELIVERED or already CANCELLED.
     */
    OrderResponse cancelOrder(Long userId, String orderStrId);

    /**
     * Requests return for a DELIVERED order.
     * Checks returnAvailable on product.
     * Creates SR return order (pickup from customer → warehouse).
     */
    OrderResponse requestReturn(Long userId, String orderStrId, String returnReason);

    /**
     * Requests exchange for a DELIVERED order.
     * Checks isExchange on product.
     * Creates SR return leg + new forward leg.
     */
    OrderResponse requestExchange(Long userId, String orderStrId, String exchangeReason);

    /**
     * Live tracking via Shiprocket AWB.
     * Throws if AWB not yet assigned.
     */
    Map<String, Object> trackOrder(Long userId, String orderStrId);

    /**
     * Get single order by orderStrId.
     * Validates order belongs to the requesting user.
     */
    OrderResponse getOrderById(Long userId, String orderStrId);

    /**
     * User facing — paginated order history, newest first.
     * Filters by logged-in userId.
     */
    Page<OrderResponse> getOrdersByUser(Long userId, int page, int size);

    /**
     * Admin — paginated orders for ONE specific user by userId.
     * Used in admin panel customer detail view.
     */
    Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size);

    /**
     * Admin — paginated ALL orders across ALL users, newest first.
     * Used in admin panel orders table.
     */
    Page<OrderSummaryResponse> getAllOrders(int page, int size);

    //get by orderId orderResponse
    public OrderResponse getOrderByOrderId(String orderId);

    OrderResponse patchOrder(Long orderId, Map<String, Object> fields);
}