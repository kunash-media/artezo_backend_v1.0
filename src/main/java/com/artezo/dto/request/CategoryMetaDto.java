package com.artezo.dto.request;

import java.util.List;

public class CategoryMetaDto {

    private List<String> productCategory;
    private List<String> productSubCategory;

    public CategoryMetaDto(){}

    public CategoryMetaDto(List<String> productCategory, List<String> productSubCategory) {
        this.productCategory = productCategory;
        this.productSubCategory = productSubCategory;
    }

    public List<String> getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(List<String> productCategory) {
        this.productCategory = productCategory;
    }

    public List<String> getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(List<String> productSubCategory) {
        this.productSubCategory = productSubCategory;
    }
}
