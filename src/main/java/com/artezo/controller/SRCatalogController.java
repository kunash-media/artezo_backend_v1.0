package com.artezo.controller;

import com.artezo.entity.ProductEntity;
import com.artezo.entity.ProductVariantEntity;
import com.artezo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SR Catalog Sync Endpoints — SR PULLS from these.
 *
 * Configure base URL in SR Checkout Dashboard → Settings → Custom Integration
 * SR calls these periodically to sync your product catalog.
 *
 * Endpoints:
 *   GET /shiprocket/products?page=1&limit=100
 *   GET /shiprocket/collections?page=1&limit=100
 *   GET /shiprocket/products?collection_id=xxx&page=1&limit=100
 */
@Slf4j
@RestController
@RequestMapping("/shiprocket")
@RequiredArgsConstructor
public class SRCatalogController {

    private final ProductRepository productRepository;

    private Map<Integer, String> categoryHashLookup() {
        List<String> allCategories = productRepository.findAllDistinctCategories(); // see note below
        Map<Integer, String> map = new HashMap<>();
        for (String cat : allCategories) {
            map.put(Math.abs(cat.hashCode()), cat);
        }
        return map;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. GET /shiprocket/products?page=1&limit=100
    //     Also handles: GET /shiprocket/products?collection_id=xxx&page=1&limit=100
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "1")   int page,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false)     String collection_id) {

        // SR uses 1-based pages; Spring Data uses 0-based
        PageRequest pageable = PageRequest.of(page - 1, limit);

        String resolvedCategory = collection_id;
        if (collection_id != null && !collection_id.isBlank()) {
            try {
                int hash = Integer.parseInt(collection_id);
                resolvedCategory = categoryHashLookup().get(hash);
                if (resolvedCategory == null) {
                    // unknown collection id — return empty instead of erroring
                    return ResponseEntity.ok(Map.of("data", Map.of("total", 0, "products", List.of())));
                }
            } catch (NumberFormatException ignored) {
                // collection_id wasn't numeric — fall through and use it as-is (defensive)
            }
        }

        Page<ProductEntity> productPage;
        if (resolvedCategory != null && !resolvedCategory.isBlank()) {
            productPage = productRepository.findByProductCategoryAndIsDeletedFalse(
                    resolvedCategory, pageable);
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
        }


        // Extract IDs from the page, then fetch with variants in one JOIN query
        // This avoids LazyInitializationException + N+1 selects on variants
        List<Long> ids = productPage.getContent().stream()
                .map(ProductEntity::getProductPrimeId)
                .toList();

        List<ProductEntity> productsWithVariants = ids.isEmpty()
                ? List.of()
                : productRepository.findByIdInWithVariants(ids);

        List<Map<String, Object>> products = productsWithVariants
                .stream()
                .map(this::mapProduct)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total",    productPage.getTotalElements());
        data.put("products", products);

        return ResponseEntity.ok(Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. GET /shiprocket/collections?page=1&limit=100
    //     SR uses these as "categories" in its checkout catalog
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/collections")
    public ResponseEntity<Map<String, Object>> getCollections(
            @RequestParam(defaultValue = "1")   int page,
            @RequestParam(defaultValue = "100") int limit) {

        // Get distinct categories from your products table
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<String> categories = productRepository.findDistinctCategories(pageable);

        List<Map<String, Object>> collections = new ArrayList<>();
        for (String cat : categories.getContent()) {
            Map<String, Object> c = new LinkedHashMap<>();
            // SR expects numeric id — use hashCode as stable int id
            c.put("id",         Math.abs(cat.hashCode()));
            c.put("title",      cat);
            c.put("handle",     cat.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            c.put("body_html",  "");
            c.put("image",      Map.of("src", ""));
            c.put("created_at", "2024-01-01T00:00:00+05:30");
            c.put("updated_at", "2024-01-01T00:00:00+05:30");
            collections.add(c);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total",       categories.getTotalElements());
        data.put("collections", collections);

        return ResponseEntity.ok(Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Map ProductEntity → SR expected product shape
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> mapProduct(ProductEntity p) {
        Map<String, Object> product = new LinkedHashMap<>();

        // SR expects numeric id — use productPrimeId
        product.put("id",           p.getProductPrimeId());
        product.put("title",        p.getProductName());
        product.put("body_html",    p.getDescription() != null ? p.getDescription() : "");
        product.put("vendor",       p.getBrandName() != null ? p.getBrandName() : "Artezo");
        product.put("product_type", p.getProductCategory() != null ? p.getProductCategory() : "");
        product.put("handle",       toHandle(p.getProductName()));
        product.put("tags",         p.getGlobalTags() != null ? p.getGlobalTags() : "");
        product.put("status",       p.getIsDeleted() ? "draft" : "active");
        product.put("created_at",   formatDate(p.getCreatedAt()));
        product.put("updated_at",   formatDate(p.getUpdatedAt()));

        // Main image
        product.put("image", Map.of("src", buildImageUrl(p.getProductPrimeId(), null)));

        // Variants
        List<Map<String, Object>> variants = new ArrayList<>();
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            for (ProductVariantEntity v : p.getVariants()) {
                variants.add(mapVariant(v, p));
            }
        } else {
            // No variants — create a single default variant
            variants.add(mapDefaultVariant(p));
        }
        product.put("variants", variants);

        // Options (Color, Size etc.)
        List<Map<String, Object>> options = buildOptions(p);
        product.put("options", options);

        return product;
    }

    private Map<String, Object> mapVariant(ProductVariantEntity v, ProductEntity p) {
        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("id",               v.getId());
        variant.put("title",            v.getTitleName() != null ? v.getTitleName() : v.getColor());
        variant.put("sku",              v.getSku());
        variant.put("price",            String.format("%.2f", v.getPrice()));
        variant.put("compare_at_price", String.format("%.2f", v.getMrp() != null ? v.getMrp() : v.getPrice()));
        variant.put("quantity",         v.getStock() != null ? v.getStock() : 0);
        variant.put("taxable",          true);
        variant.put("grams",            toGrams(v.getWeight()));
        variant.put("weight",           v.getWeight() != null ? v.getWeight() : 0.5);
        variant.put("weight_unit",      "kg");
        variant.put("created_at",       formatDate(p.getCreatedAt()));
        variant.put("updated_at",       formatDate(p.getUpdatedAt()));
        // Use variant image if available, fallback to product main image
        String variantImgSrc = (v.getMainImageData() != null && v.getMainImageData().length > 0)
                ? buildImageUrl(p.getProductPrimeId(), v.getVariantId())
                : buildImageUrl(p.getProductPrimeId(), null);
        variant.put("image", Map.of("src", variantImgSrc));

        // option_values map
        Map<String, Object> optVals = new LinkedHashMap<>();
        if (v.getColor() != null) optVals.put("Color", v.getColor());
        if (v.getSize()  != null) optVals.put("Size",  v.getSize());
        variant.put("option_values", optVals);

        return variant;
    }

    private Map<String, Object> mapDefaultVariant(ProductEntity p) {
        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("id",               p.getProductPrimeId());
        variant.put("title",            "Default");
        variant.put("sku",              p.getCurrentSku() != null ? p.getCurrentSku() : p.getProductStrId());
        variant.put("price",            String.format("%.2f", p.getCurrentSellingPrice() != null ? p.getCurrentSellingPrice() : 0.0));
        variant.put("compare_at_price", String.format("%.2f", p.getCurrentMrpPrice()    != null ? p.getCurrentMrpPrice()    : 0.0));
        variant.put("quantity",         p.getCurrentStock() != null ? p.getCurrentStock() : 0);
        variant.put("taxable",          true);
        variant.put("grams",            toGrams(p.getWeight()));
        variant.put("weight",           p.getWeight() != null ? p.getWeight() : 0.5);
        variant.put("weight_unit",      "kg");
        variant.put("created_at",       formatDate(p.getCreatedAt()));
        variant.put("updated_at",       formatDate(p.getUpdatedAt()));
        variant.put("image",            Map.of("src", buildImageUrl(p.getProductPrimeId(), null)));
        variant.put("option_values",    new LinkedHashMap<>());
        return variant;
    }

    private List<Map<String, Object>> buildOptions(ProductEntity p) {
        List<Map<String, Object>> options = new ArrayList<>();
        if (p.getVariants() == null || p.getVariants().isEmpty()) return options;

        Set<String> colors = new LinkedHashSet<>();
        Set<String> sizes  = new LinkedHashSet<>();
        for (ProductVariantEntity v : p.getVariants()) {
            if (v.getColor() != null) colors.add(v.getColor());
            if (v.getSize()  != null) sizes.add(v.getSize());
        }
        if (!colors.isEmpty()) options.add(Map.of("name", "Color", "values", new ArrayList<>(colors)));
        if (!sizes.isEmpty())  options.add(Map.of("name", "Size",  "values", new ArrayList<>(sizes)));

        return options;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build image URL — since you store images as byte[] in DB,
     * point to your existing image-serving endpoint.
     * Replace with your actual image URL pattern.
     */
    private String buildImageUrl(Long productPrimeId, String variantId) {
        String base = "https://artezo-api.artezo.in/api/products/" + productPrimeId + "/main";
        if (variantId != null && !variantId.isBlank()) {
            return "https://artezo-api.artezo.in/api/products/" + productPrimeId + "/variant/" + variantId + "/main";
        }
        return base;
    }

    private String toHandle(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private int toGrams(Double weightKg) {
        if (weightKg == null) return 500;
        return (int) (weightKg * 1000);
    }

    private String formatDate(java.time.LocalDateTime dt) {
        if (dt == null) return "2024-01-01T00:00:00+05:30";
        return dt.toString() + "+05:30";
    }
}