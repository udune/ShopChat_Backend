package com.cMall.feedShop.cart.application.service;

import com.cMall.feedShop.cart.application.dto.response.WishListResponse;
import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import com.cMall.feedShop.cart.domain.exception.CartException;
import com.cMall.feedShop.cart.domain.repository.WishlistRepository;
import com.cMall.feedShop.cart.infrastructure.repository.WishlistQueryRepository;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.utils.PagingUtils;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;

    // 찜한 상품 목록 조회
    public WishListResponse getWishList(int page, int size, String loginId) {
        // 1. 사용자 조회
        User user = getCurrentUser(loginId);

        // 2. 전체 찜한 상품 개수 조회
        long totalElements = wishlistRepository.countWishlistByUserId(user.getId());

        // 3. 페이징 검증
        Pageable pageable = PagingUtils.createPageable(page, size, totalElements);

        // 4. 찜 목록 조회
        Page<WishlistInfo> wishlistPage = wishlistRepository.findWishlistByUserId(user.getId(), pageable);

        // 5. 결과 반환
        return WishListResponse.of(wishlistPage);
    }

    /**
     * 현재 로그인한 사용자의 정보를 가져옵니다.
     *
     * @param loginId 현재 로그인한 사용자의 ID
     * @return User 객체
     */
    private User getCurrentUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CartException(ErrorCode.USER_NOT_FOUND));
    }
}
