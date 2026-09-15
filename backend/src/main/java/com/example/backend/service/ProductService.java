package com.example.backend.service;

import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAll(String genre) {
        List<Product> products = genre == null || genre.isBlank()
                ? productRepository.findAllByOrderByProductIdAsc()
                : productRepository.findAllByGenreIgnoreCaseOrderByProductIdAsc(genre.trim());
        return products.stream().map(ProductResponse::from).toList();
    }

    public Product findEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("商品が見つかりません。"));
    }

    public ProductResponse findOne(Long productId) {
        return ProductResponse.from(findEntity(productId));
    }
}
