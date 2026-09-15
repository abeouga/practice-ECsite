package com.example.backend.dto;

import com.example.backend.entity.CartItem;

public record CartItemResponse(Long cartId, ProductResponse product, int quantity, long subtotal) {
    public static CartItemResponse from(CartItem item) {
        long subtotal = Math.multiplyExact(item.getProduct().getPrice(), item.getQuantity());
        return new CartItemResponse(item.getCartId(), ProductResponse.from(item.getProduct()), item.getQuantity(), subtotal);
    }
}
