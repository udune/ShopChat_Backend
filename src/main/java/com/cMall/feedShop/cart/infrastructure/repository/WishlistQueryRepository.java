package com.cMall.feedShop.cart.infrastructure.repository;

public interface WishlistQueryRepository {

    // 찜 수 증가
    void increaseWishCount(Long productId);

    // 찜 수 감소
    void decreaseWishCount(Long productId);

    // 실제 찜 수와 카운터 동기화
    void syncWishCount(Long productId);
}
