package com.example.backend.dto;

public record MeResponse(boolean authenticated, UserResponse user) {
}
