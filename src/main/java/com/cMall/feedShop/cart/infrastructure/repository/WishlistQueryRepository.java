package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.domain.model.WishList;

import java.util.Optional;

public interface WishlistQueryRepository {

    // 사용자 ID와 상품 ID로 위시리스트 항목을 조회
    Optional<WishList> findByUserIdAndProductId(Long userId, Long productId);
}
