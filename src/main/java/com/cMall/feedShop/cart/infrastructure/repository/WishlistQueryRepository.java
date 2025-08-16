package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistQueryRepository {

    // 사용자와 상품으로 활성 찜 정보 조회
    boolean existsActiveWishlistByUserIdAndProductId(Long userId, Long productId);

    // 사용자 ID로 위시리스트 조회
    Page<WishlistInfo> findWishlistByUserId(Long userId, Pageable pageable);

    // 사용자 ID로 위시리스트 개수 조회
    long countWishlistByUserId(Long userId);
}
