package com.example.backend.controller;

import com.example.backend.dto.PurchaseResponse;
import com.example.backend.service.PurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<List<PurchaseResponse>> purchase(Authentication authentication) {
        List<PurchaseResponse> response = purchaseService.purchaseCart(authentication);
        return ResponseEntity.created(URI.create("/api/purchases")).body(response);
    }

    @GetMapping
    public List<PurchaseResponse> history(Authentication authentication) {
        return purchaseService.findHistory(authentication);
    }
}
