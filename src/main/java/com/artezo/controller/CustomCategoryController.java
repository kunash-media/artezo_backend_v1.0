package com.artezo.controller;

import com.artezo.dto.request.CustomCategoryRequest;
import com.artezo.dto.response.CustomCategoryResponse;
import com.artezo.service.CustomCategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/custom-categories")
public class CustomCategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CustomCategoryController.class);

    private final CustomCategoryService service;

    public CustomCategoryController(CustomCategoryService service) {
        this.service = service;
    }

    @PostMapping("/adhoc/create")
    public ResponseEntity<CustomCategoryResponse> create(@Valid @RequestBody CustomCategoryRequest request) {
        logger.info("POST /api/v1/custom-categories - Creating category");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomCategoryResponse> getById(@PathVariable Long id) {
        logger.info("GET /api/v1/custom-categories/{}", id);
        return ResponseEntity.ok(service.getCategoryById(id));
    }

    @GetMapping("/get-all-categories")
    public ResponseEntity<List<CustomCategoryResponse>> getAll() {
        logger.info("GET /api/v1/custom-categories - Fetching all categories");
        return ResponseEntity.ok(service.getAllCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomCategoryRequest request) {
        logger.info("PUT /api/v1/custom-categories/{}", id);
        return ResponseEntity.ok(service.updateCategory(id, request));
    }

    @PatchMapping("/patch-category/{id}")
    public ResponseEntity<CustomCategoryResponse> patch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> fields) {
        logger.info("PATCH /api/v1/custom-categories/{}", id);
        return ResponseEntity.ok(service.patchCategory(id, fields));
    }

    @DeleteMapping("/delete-category/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        logger.info("DELETE /api/v1/custom-categories/{}", id);
        return ResponseEntity.ok(service.deleteCategory(id));
    }
}