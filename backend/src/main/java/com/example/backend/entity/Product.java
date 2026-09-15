package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 80)
    private String genre;

    @Column(nullable = false)
    private int stock;

    @Column(length = 1000)
    private String image;

    @Column(length = 2000)
    private String description;

    protected Product() {
    }

    public Product(String name, long price, String genre, int stock, String image, String description) {
        this.name = name;
        this.price = price;
        this.genre = genre;
        this.stock = stock;
        this.image = image;
        this.description = description;
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public String getGenre() {
        return genre;
    }

    public int getStock() {
        return stock;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }
}
