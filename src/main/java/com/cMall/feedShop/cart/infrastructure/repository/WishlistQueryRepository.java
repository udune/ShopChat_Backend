package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistQueryRepository {

    // 사용자 ID로 위시리스트 조회
    Page<WishlistInfo> findWishlistByUserId(Long userId, Pageable pageable);
}
