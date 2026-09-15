package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull(message = "商品IDを指定してください。") @Positive(message = "商品IDが正しくありません。") Long productId,
        @NotNull(message = "数量を指定してください。") @Positive(message = "数量は1以上で指定してください。") Integer quantity
) {
}
