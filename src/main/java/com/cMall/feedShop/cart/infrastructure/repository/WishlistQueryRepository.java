package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.domain.model.WishList;

import java.util.Optional;

public interface WishlistQueryRepository {

    // 찜 수 감소
    void decreaseWishCount(Long productId);

    // 사용자 ID와 상품 ID로 찜 목록 조회 (삭제되지 않은 것만)
    Optional<WishList> findByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);

}

