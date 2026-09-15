package com.example.backend.controller;

import com.example.backend.dto.ProductResponse;
import com.example.backend.service.ProductService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> findAll(@RequestParam(required = false) String genre) {
        return productService.findAll(genre);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> findOne(@PathVariable @Positive Long productId) {
        return ResponseEntity.ok(productService.findOne(productId));
    }
}
