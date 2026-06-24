package com.artezo.dto.request;

import lombok.Data;
import java.util.List;

/**
 * BOUGHT-TOGETHER: wraps multiple AddToCartRequest items so they can be
 * submitted in a single POST. Each item reuses the exact same fields/semantics
 * as the existing single add-to-cart flow — no new validation rules introduced.
 */
@Data
public class AddMultipleToCartRequest {

    private List<AddToCartRequest> items;


    public AddMultipleToCartRequest(){}

    public AddMultipleToCartRequest(List<AddToCartRequest> items) {
        this.items = items;
    }

    public List<AddToCartRequest> getItems() {
        return items;
    }

    public void setItems(List<AddToCartRequest> items) {
        this.items = items;
    }
}