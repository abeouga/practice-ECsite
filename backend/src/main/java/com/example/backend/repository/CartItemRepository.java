package com.example.backend.repository;

import com.example.backend.entity.CartItem;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select c from CartItem c join fetch c.product where c.user.userId = :userId order by c.cartId")
    List<CartItem> findAllByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CartItem c join fetch c.product where c.user.userId = :userId order by c.cartId")
    List<CartItem> findAllByUserIdForUpdate(@Param("userId") Long userId);
}
