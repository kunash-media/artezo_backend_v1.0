package com.artezo.service;


import com.artezo.dto.request.CustomCategoryRequest;
import com.artezo.dto.response.CustomCategoryResponse;

import java.util.List;
import java.util.Map;

public interface CustomCategoryService {

    CustomCategoryResponse createCategory(CustomCategoryRequest request);

    CustomCategoryResponse getCategoryById(Long categoryId);

    List<CustomCategoryResponse> getAllCategories();

    CustomCategoryResponse updateCategory(Long categoryId, CustomCategoryRequest request);

    CustomCategoryResponse patchCategory(Long categoryId, Map<String, Object> fields);

    String deleteCategory(Long categoryId);
}