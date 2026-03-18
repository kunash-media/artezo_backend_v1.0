package com.artezo.service.serviceImpl;

import com.artezo.dto.request.CustomCategoryRequest;
import com.artezo.dto.response.CustomCategoryResponse;
import com.artezo.entity.CustomCategoryEntity;

import com.artezo.exceptions.DuplicateCategoryException;
import com.artezo.exceptions.ResourceNotFoundException;
import com.artezo.repository.CustomCategoryRepository;
import com.artezo.service.CustomCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Service
@Transactional
public class CustomCategoryServiceImpl implements CustomCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CustomCategoryServiceImpl.class);
    private final CustomCategoryRepository repository;

    public CustomCategoryServiceImpl(CustomCategoryRepository repository) {
        this.repository = repository;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CustomCategoryResponse createCategory(CustomCategoryRequest request) {
        logger.info("Creating new category with name: {}", request.getProductCategory());

        if (repository.existsByProductCategoryIgnoreCase(request.getProductCategory())) {
            logger.warn("Duplicate category name attempted: {}", request.getProductCategory());
            try {
                throw new DuplicateCategoryException("Category with name '" + request.getProductCategory() + "' already exists");
            } catch (DuplicateCategoryException e) {
                throw new RuntimeException(e);
            }
        }

        CustomCategoryEntity entity = mapToEntity(request);
        CustomCategoryEntity saved = repository.save(entity);

        logger.info("Category created successfully with ID: {}", saved.getCategoryId());
        return mapToResponse(saved);
    }

    // ─── READ BY ID ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CustomCategoryResponse getCategoryById(Long categoryId) {
        logger.info("Fetching category with ID: {}", categoryId);

        CustomCategoryEntity entity = null;
        try {
            entity = repository.findById(categoryId)
                    .orElseThrow(() -> {
                        logger.error("Category not found with ID: {}", categoryId);
                        return new ResourceNotFoundException("No category found with ID: " + categoryId);
                    });
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException(e);
        }

        return mapToResponse(entity);
    }

    // ─── READ ALL ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<CustomCategoryResponse> getAllCategories() {
        logger.info("Fetching all categories");

        List<CustomCategoryEntity> categories = repository.findAll();

        if (categories.isEmpty()) {
            logger.warn("No categories found in the system");
        }

        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── FULL UPDATE (PUT) ────────────────────────────────────────────────────

    @Override
    @Transactional
    public CustomCategoryResponse updateCategory(Long categoryId, CustomCategoryRequest request) {
        logger.info("Updating category with ID: {}", categoryId);

        CustomCategoryEntity entity = null;
        try {
            entity = repository.findById(categoryId)
                    .orElseThrow(() -> {
                        logger.error("Category not found for update with ID: {}", categoryId);
                        return new ResourceNotFoundException("No category found with ID: " + categoryId);
                    });
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Duplication check — skip if same entity
        boolean nameChanged = !entity.getProductCategory().equalsIgnoreCase(request.getProductCategory());
        if (nameChanged && repository.existsByProductCategoryIgnoreCase(request.getProductCategory())) {
            logger.warn("Category name '{}' already in use during update", request.getProductCategory());
            try {
                throw new DuplicateCategoryException("Category with name '" + request.getProductCategory() + "' already exists");
            } catch (DuplicateCategoryException e) {
                throw new RuntimeException(e);
            }
        }

        entity.setProductCategory(request.getProductCategory());
        entity.setCategoryPath(request.getCategoryPath());
        entity.setProductCategoryRedirect(request.getProductCategoryRedirect());
        entity.setCategoryPathRedirect(request.getCategoryPathRedirect());
        entity.setApproved(request.getApproved());
        entity.setTrendingMark(request.getTrendingMark());

        CustomCategoryEntity updated = repository.save(entity);
        logger.info("Category updated successfully with ID: {}", updated.getCategoryId());
        return mapToResponse(updated);
    }

    // ─── PARTIAL UPDATE (PATCH) ───────────────────────────────────────────────

    @Override
    @Transactional
    public CustomCategoryResponse patchCategory(Long categoryId, Map<String, Object> fields) {
        logger.info("Patching category with ID: {}", categoryId);

        CustomCategoryEntity entity = repository.findById(categoryId)
                .orElseThrow(() -> {
                    logger.error("Category not found for patch with ID: {}", categoryId);
                    return new ResourceNotFoundException("No category found with ID: " + categoryId);
                });

        fields.forEach((key, value) -> {
            switch (key) {
                case "productCategory" -> {
                    String newName = (String) value;
                    if (!entity.getProductCategory().equalsIgnoreCase(newName)
                            && repository.existsByProductCategoryIgnoreCase(newName)) {
                        logger.warn("Patch rejected — category name '{}' already exists", newName);
                        throw new DuplicateCategoryException("Category with name '" + newName + "' already exists");
                    }
                    entity.setProductCategory(newName);
                }
                case "categoryPath" -> entity.setCategoryPath((List<String>) value);
                case "productCategoryRedirect" -> entity.setProductCategoryRedirect((String) value);
                case "categoryPathRedirect" -> entity.setCategoryPathRedirect((String) value);
                case "approved" -> entity.setApproved((Boolean) value);
                case "trendingMark" -> entity.setTrendingMark((Boolean) value);
                default -> logger.warn("Unknown field '{}' ignored during patch", key);
            }
        });

        CustomCategoryEntity patched = repository.save(entity);
        logger.info("Category patched successfully with ID: {}", patched.getCategoryId());
        return mapToResponse(patched);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Override
    public String deleteCategory(Long categoryId) {
        logger.info("Deleting category with ID: {}", categoryId);

        if (!repository.existsById(categoryId)) {
            logger.error("Category not found for deletion with ID: {}", categoryId);
            try {
                throw new ResourceNotFoundException("No category found with ID: " + categoryId);
            } catch (ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        repository.deleteById(categoryId);
        logger.info("Category deleted successfully with ID: {}", categoryId);
        return "Category with ID " + categoryId + " has been deleted successfully";
    }

    // ─── MAPPERS ──────────────────────────────────────────────────────────────

    private CustomCategoryEntity mapToEntity(CustomCategoryRequest request) {
        CustomCategoryEntity entity = new CustomCategoryEntity();
        entity.setProductCategory(request.getProductCategory());
        entity.setCategoryPath(request.getCategoryPath());
        entity.setProductCategoryRedirect(request.getProductCategoryRedirect());
        entity.setCategoryPathRedirect(request.getCategoryPathRedirect());
        entity.setApproved(request.getApproved());
        entity.setTrendingMark(request.getTrendingMark());
        return entity;
    }

    private CustomCategoryResponse mapToResponse(CustomCategoryEntity entity) {
        return CustomCategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .productCategory(entity.getProductCategory())
                .categoryPath(new ArrayList<>(entity.getCategoryPath())) // ← forces load
                .productCategoryRedirect(entity.getProductCategoryRedirect())
                .categoryPathRedirect(entity.getCategoryPathRedirect())
                .createdAt(entity.getCreatedAt())
                .approved(entity.getApproved())
                .trendingMark(entity.getTrendingMark())
                .build();
    }
}