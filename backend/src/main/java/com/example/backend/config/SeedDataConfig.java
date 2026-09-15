package com.example.backend.config;

import com.example.backend.entity.AppUser;
import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@Profile("!test")
public class SeedDataConfig {
    @Bean
    CommandLineRunner seedData(UserRepository userRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.saveAll(List.of(
                        new AppUser("alice@example.com", passwordEncoder.encode("password"), "Alice"),
                        new AppUser("bob@example.com", passwordEncoder.encode("password"), "Bob")));
            }
            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                        new Product("Morning Coffee", 680L, "食品", 12, "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800", "香りのよい深煎りコーヒーです。"),
                        new Product("Green Tea", 520L, "食品", 8, "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=800", "毎日の食事に合う日本茶です。"),
                        new Product("Notebook", 380L, "文具", 20, "https://images.unsplash.com/photo-1517842645767-c639042777db?w=800", "学習記録に使えるノートです。"),
                        new Product("Desk Lamp", 2980L, "生活用品", 0, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800", "手元を照らすシンプルなデスクライトです.")));
            }
        };
    }
}
