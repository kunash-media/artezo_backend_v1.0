package com.artezo.service;

import com.artezo.dto.request.ProductCardSnapshotDto;
import com.artezo.entity.ProductEntity;
import com.artezo.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);


    public SuggestionService(RedisTemplate<String, String> redisTemplate, ProductRepository productRepository, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    private static final String RECENT_KEY = "recentviewed:";
    private static final String CARD_KEY   = "product:card:";
    private static final int    MAX_SUGGESTIONS = 10;

    public List<ProductCardSnapshotDto> getSuggestions(Long userId, Long currentProductId,
                                                       String currentCategory, String currentSubCategory) {
        try {
            // ── Phase 2: Redis powered ─────────────────────────────────────
            if (userId != null) {
                List<ProductCardSnapshotDto> personalised =
                        getPersonalisedSuggestions(userId, currentProductId);

                if (!personalised.isEmpty()) {
                    log.info("Phase 2 suggestions returned {} products for userId: {}", personalised.size(), userId);
                    return personalised;
                }
            }

            // ── Phase 1: Fallback — same category ─────────────────────────
            log.info("Falling back to Phase 1 suggestions for productId: {}", currentProductId);
            return getFallbackSuggestions(currentProductId, currentCategory, currentSubCategory);

        } catch (Exception e) {
            log.error("Failed to fetch suggestions for userId: {} | productId: {} | reason: {}",
                    userId, currentProductId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Phase 2 logic ──────────────────────────────────────────────────────
    private List<ProductCardSnapshotDto> getPersonalisedSuggestions(Long userId, Long currentProductId) {
        try {
            String recentKey = RECENT_KEY + userId;

            // Step 1: Get recent viewed productIds from Redis
            Set<String> recentIds = redisTemplate.opsForZSet()
                    .reverseRange(recentKey, 0, 9);

            if (recentIds == null || recentIds.isEmpty()) {
                log.info("No Redis recent views for userId: {} — will fallback", userId);
                return Collections.emptyList();
            }

            // Step 2: Batch fetch card snapshots
            List<String> cardKeys = recentIds.stream()
                    .map(id -> CARD_KEY + id)
                    .collect(Collectors.toList());

            List<String> jsonList = redisTemplate.opsForValue().multiGet(cardKeys);

            if (jsonList == null || jsonList.isEmpty()) {
                return Collections.emptyList();
            }

            // Step 3: Extract unique categories + subCategories
            List<String> categories    = new ArrayList<>();
            List<String> subCategories = new ArrayList<>();
            List<Long>   excludedIds   = new ArrayList<>();

            // Always exclude current product
            excludedIds.add(currentProductId);

            for (String json : jsonList) {
                if (json != null) {
                    ProductCardSnapshotDto card = objectMapper.readValue(json, ProductCardSnapshotDto.class);

                    if (card.getProductCategory() != null
                            && !categories.contains(card.getProductCategory())) {
                        categories.add(card.getProductCategory());
                    }
                    if (card.getProductSubCategory() != null
                            && !subCategories.contains(card.getProductSubCategory())) {
                        subCategories.add(card.getProductSubCategory());
                    }
                    // Exclude all recently viewed from suggestions
                    if (card.getProductPrimeId() != null) {
                        excludedIds.add(card.getProductPrimeId());
                    }
                }
            }

            if (categories.isEmpty() && subCategories.isEmpty()) {
                return Collections.emptyList();
            }

            // Step 4: Query DB
            List<ProductEntity> results = productRepository.findSuggestions(
                    categories,
                    subCategories,
                    excludedIds,
                    PageRequest.of(0, MAX_SUGGESTIONS)
            );

            // Step 5: Map to card snapshots
            return results.stream()
                    .map(this::mapToCardSnapshot)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Phase 2 suggestion failed for userId: {} | reason: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Phase 1 fallback logic ─────────────────────────────────────────────
    private List<ProductCardSnapshotDto> getFallbackSuggestions(Long currentProductId,
                                                                String category, String subCategory) {
        List<ProductEntity> results = productRepository.findSuggestionsFallback(
                category,
                subCategory,
                currentProductId,
                PageRequest.of(0, MAX_SUGGESTIONS)
        );

        return results.stream()
                .map(this::mapToCardSnapshot)
                .collect(Collectors.toList());
    }

    // ── Map entity → card snapshot ─────────────────────────────────────────
    private ProductCardSnapshotDto mapToCardSnapshot(ProductEntity p) {
        return ProductCardSnapshotDto.builder()
                .productPrimeId(p.getProductPrimeId())
                .productStrId(p.getProductStrId())
                .productName(p.getProductName())
                .brandName(p.getBrandName())
                .mainImage("/api/products/" + p.getProductPrimeId() + "/main")
                .currentSellingPrice(p.getCurrentSellingPrice())
                .currentMrpPrice(p.getCurrentMrpPrice())
                .currentSku(p.getCurrentSku())
                .productCategory(p.getProductCategory())
                .productSubCategory(p.getProductSubCategory())
                .currentStock(p.getCurrentStock())
                .hasVariants(p.getHasVariants())
                .selectedColor(p.getSelectedColor())
                .viewedAt(null) // not needed for suggestions
                .build();
    }
}