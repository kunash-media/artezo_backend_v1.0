package com.artezo.service;

import com.artezo.dto.request.ProductCardSnapshotDto;
import com.artezo.dto.response.ProductResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecentViewService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(RecentViewService.class);


    public RecentViewService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.redis.recent-view.ttl}")
    private long recentViewTtl;

    @Value("${app.redis.product-card.ttl}")
    private long productCardTtl;

    @Value("${app.redis.max-recent}")
    private int maxRecent;

    private static final String RECENT_KEY   = "recentviewed:";
    private static final String CARD_KEY     = "product:card:";

    // Called async from product service
    public void recordView(Long userId, ProductResponseDto product) {
        try {
            String recentKey = RECENT_KEY + userId;
            String cardKey   = CARD_KEY + product.getProductPrimeId();
            long   timestamp = System.currentTimeMillis();

            // 1. Build snapshot
            ProductCardSnapshotDto snapshot = ProductCardSnapshotDto.builder()
                    .productPrimeId(product.getProductPrimeId())
                    .productStrId(product.getProductStrId())
                    .productName(product.getProductName())
                    .brandName(product.getBrandName())
                    .mainImage(product.getMainImage())
                    .currentSellingPrice(product.getCurrentSellingPrice())
                    .currentMrpPrice(product.getCurrentMrpPrice())
                    .currentSku(product.getCurrentSku())
                    .productCategory(product.getProductCategory())
                    .productSubCategory(product.getProductSubCategory())
                    .currentStock(product.getCurrentStock())
                    .hasVariants(product.getHasVariants())
                    .selectedColor(product.getSelectedColor())
                    .viewedAt(timestamp)
                    .build();

            String json = objectMapper.writeValueAsString(snapshot);

            // 2. Store card snapshot with 24h TTL
            redisTemplate.opsForValue().set(cardKey, json, productCardTtl, TimeUnit.SECONDS);

            // 3. Add to user's sorted set (score = timestamp for ordering)
            redisTemplate.opsForZSet().add(recentKey, String.valueOf(product.getProductPrimeId()), timestamp);

            // 4. Trim to max 10 — remove oldest beyond limit
            redisTemplate.opsForZSet().removeRange(recentKey, 0, -(maxRecent + 1));

            // 5. Refresh TTL on sorted set (30 days)
            redisTemplate.expire(recentKey, recentViewTtl, TimeUnit.SECONDS);

            log.info("Recent view recorded for userId: {} | productId: {}", userId, product.getProductPrimeId());

        } catch (Exception e) {
            // Never let Redis failure affect main flow
            log.error("Failed to record recent view for userId: {} | productId: {} | reason: {}",
                    userId, product.getProductPrimeId(), e.getMessage());
        }
    }

    // Called by recent-viewed API endpoint
    public List<ProductCardSnapshotDto> getRecentViewed(Long userId) {
        try {
            String recentKey = RECENT_KEY + userId;

            // 1. Get last 10 productIds, newest first
            Set<String> productIds = redisTemplate.opsForZSet()
                    .reverseRange(recentKey, 0, maxRecent - 1);

            if (productIds == null || productIds.isEmpty()) {
                log.info("No recent views found in Redis for userId: {}", userId);
                return Collections.emptyList();
            }

            // 2. Batch fetch all card snapshots
            List<String> cardKeys = productIds.stream()
                    .map(id -> CARD_KEY + id)
                    .collect(Collectors.toList());

            List<String> jsonList = redisTemplate.opsForValue().multiGet(cardKeys);

            // 3. Parse and return, skip nulls (expired cards)
            List<ProductCardSnapshotDto> result = new ArrayList<>();
            if (jsonList != null) {
                for (String json : jsonList) {
                    if (json != null) {
                        result.add(objectMapper.readValue(json, ProductCardSnapshotDto.class));
                    }
                }
            }

            log.info("Fetched {} recent viewed products for userId: {}", result.size(), userId);
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch recent views for userId: {} | reason: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}