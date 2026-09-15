package com.example.backend.dto;

import com.example.backend.entity.Purchase;

import java.time.Instant;

public record PurchaseResponse(
        Long purchaseId,
        ProductResponse product,
        int quantity,
        long price,
        long subtotal,
        Instant purchasedAt
) {
    public static PurchaseResponse from(Purchase purchase) {
        long subtotal = Math.multiplyExact(purchase.getPrice(), purchase.getQuantity());
        return new PurchaseResponse(
                purchase.getPurchaseId(), ProductResponse.from(purchase.getProduct()), purchase.getQuantity(),
                purchase.getPrice(), subtotal, purchase.getPurchasedAt());
    }
}
