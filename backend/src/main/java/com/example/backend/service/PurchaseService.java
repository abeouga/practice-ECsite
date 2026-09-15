package com.example.backend.service;

import com.example.backend.dto.PurchaseResponse;
import com.example.backend.entity.AppUser;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Purchase;
import com.example.backend.exception.ConflictException;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.PurchaseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final Clock clock;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            CartItemRepository cartItemRepository,
            UserService userService,
            Clock clock) {
        this.purchaseRepository = purchaseRepository;
        this.cartItemRepository = cartItemRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public List<PurchaseResponse> purchaseCart(Authentication authentication) {
        AppUser user = userService.requireUserForUpdate(authentication);
        List<CartItem> cartItems = cartItemRepository.findAllByUserIdForUpdate(user.getUserId());
        if (cartItems.isEmpty()) {
            throw new ConflictException("カートに商品がありません。");
        }

        Instant purchasedAt = clock.instant();
        List<Purchase> purchases = new ArrayList<>(cartItems.size());
        for (CartItem item : cartItems) {
            Math.multiplyExact(item.getProduct().getPrice(), item.getQuantity());
            purchases.add(new Purchase(user, item.getProduct(), item.getQuantity(), item.getProduct().getPrice(), purchasedAt));
        }

        List<Purchase> savedPurchases = purchaseRepository.saveAll(purchases);
        purchaseRepository.flush();
        cartItemRepository.deleteAll(cartItems);
        cartItemRepository.flush();
        return savedPurchases.stream().map(PurchaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> findHistory(Authentication authentication) {
        AppUser user = userService.requireUser(authentication);
        return purchaseRepository.findAllByUserUserIdOrderByPurchasedAtDescPurchaseIdDesc(user.getUserId())
                .stream().map(PurchaseResponse::from).toList();
    }
}
