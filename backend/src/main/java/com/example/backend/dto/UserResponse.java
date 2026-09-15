package com.example.backend.dto;

import com.example.backend.entity.AppUser;

public record UserResponse(Long userId, String email, String name) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getUserId(), user.getEmail(), user.getName());
    }
}
