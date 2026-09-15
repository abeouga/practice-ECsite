package com.example.backend.service;

import com.example.backend.dto.AddCartItemRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.CartResponse;
import com.example.backend.entity.AppUser;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.repository.CartItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserService userService;

    public CartService(CartItemRepository cartItemRepository, ProductService productService, UserService userService) {
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Authentication authentication) {
        AppUser user = userService.requireUser(authentication);
        return toResponse(cartItemRepository.findAllByUserId(user.getUserId()));
    }

    @Transactional
    public CartItemResponse addItem(Authentication authentication, AddCartItemRequest request) {
        AppUser user = userService.requireUserForUpdate(authentication);
        if (request.quantity() < 1) {
            throw new BadRequestException("数量は1以上で指定してください。");
        }
        Product product = productService.findEntity(request.productId());
        if (product.getStock() <= 0) {
            throw new ConflictException("在庫切れの商品はカートに入れられません。");
        }
        return CartItemResponse.from(cartItemRepository.save(new CartItem(user, product, request.quantity())));
    }

    private CartResponse toResponse(List<CartItem> cartItems) {
        List<CartItemResponse> items = cartItems.stream().map(CartItemResponse::from).toList();
        long total = 0;
        for (CartItemResponse item : items) {
            total = Math.addExact(total, item.subtotal());
        }
        return new CartResponse(items, total);
    }
}
