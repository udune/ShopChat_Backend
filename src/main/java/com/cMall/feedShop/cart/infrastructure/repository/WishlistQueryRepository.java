package com.cMall.feedShop.cart.infrastructure.repository;

public interface WishlistQueryRepository {

    // 사용자와 상품으로 활성 찜 정보 조회
    boolean existsActiveWishlistByUserIdAndProductId(Long userId, Long productId);
}
