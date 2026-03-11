package com.artezo.controller;

import com.artezo.dto.request.ProductCardSnapshotDto;
import com.artezo.service.RecentViewService;
import com.artezo.service.SuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recent-users")
public class RecentViewController {

    private final RecentViewService recentViewService;
    private final SuggestionService suggestionService;
    private static final Logger log = LoggerFactory.getLogger(RecentViewController.class);

    public RecentViewController(RecentViewService recentViewService, SuggestionService suggestionService) {
        this.recentViewService = recentViewService;
        this.suggestionService = suggestionService;
    }

    //-------------------------------------------//
    //            Recent view product api        //
    //-------------------------------------------//
    @GetMapping("/{userId}/recent-viewed")
    public ResponseEntity<?> getRecentViewed(@PathVariable Long userId) {
        log.info("GET /api/users/{}/recent-viewed", userId);

        try {
            List<ProductCardSnapshotDto> recentViewed = recentViewService.getRecentViewed(userId);

            if (recentViewed.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No recent viewed products found",
                        "data", Collections.emptyList()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Recent viewed products fetched successfully",
                    "count", recentViewed.size(),
                    "data", recentViewed
            ));

        } catch (Exception e) {
            log.error("Failed to fetch recent viewed for userId: {} | reason: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch recent viewed products",
                            "message", e.getMessage()
                    ));
        }
    }

    //--------------------------------------//
    //          Suggestion product  api     //
    //--------------------------------------//
    @GetMapping("/suggestions-product")
    public ResponseEntity<?> getSuggestions(
            @RequestParam Long productId,
            @RequestParam String category,
            @RequestParam String subCategory,
            @RequestParam(required = false) Long userId) {

        log.info("GET /suggestions | productId: {} | userId: {} | category: {}", productId, userId, category);

        try {
            List<ProductCardSnapshotDto> suggestions =
                    suggestionService.getSuggestions(userId, productId, category, subCategory);

            if (suggestions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No suggestions found",
                        "count", 0,
                        "data", Collections.emptyList()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Suggestions fetched successfully",
                    "count", suggestions.size(),
                    "data", suggestions
            ));

        } catch (Exception e) {
            log.error("Suggestion fetch failed | productId: {} | reason: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch suggestions",
                            "message", e.getMessage()
                    ));
        }
    }
}