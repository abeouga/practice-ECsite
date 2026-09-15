package com.example.backend.dto;

import com.example.backend.entity.Product;

public record ProductResponse(
        Long productId,
        String name,
        long price,
        String genre,
        int stock,
        String image,
        String description
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(), product.getName(), product.getPrice(), product.getGenre(),
                product.getStock(), product.getImage(), product.getDescription());
    }
}
