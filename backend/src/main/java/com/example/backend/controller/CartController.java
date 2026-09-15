package com.example.backend.controller;

import com.example.backend.dto.AddCartItemRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.CartResponse;
import com.example.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication);
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request) {
        CartItemResponse response = cartService.addItem(authentication, request);
        return ResponseEntity.created(URI.create("/api/cart/items/" + response.cartId())).body(response);
    }
}
