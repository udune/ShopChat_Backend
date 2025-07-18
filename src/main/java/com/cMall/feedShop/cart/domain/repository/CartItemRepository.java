package com.cMall.feedShop.cart.domain.repository;

import com.cMall.feedShop.cart.domain.model.Cart;
import com.cMall.feedShop.cart.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndOptionIdAndImageId(Cart cart, Long optionId, Long imageId);

    List<CartItem> findByCart_User_IdOrderByCreatedAtDesc(Long userId);
}
