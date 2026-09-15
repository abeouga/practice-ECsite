package com.example.backend.dto;

import java.util.List;

public record CartResponse(List<CartItemResponse> items, long totalAmount) {
}
