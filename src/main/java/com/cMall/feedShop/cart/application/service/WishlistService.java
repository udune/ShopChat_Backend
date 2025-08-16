package com.cMall.feedShop.cart.application.service;

import com.cMall.feedShop.cart.application.dto.request.WishListRequest;
import com.cMall.feedShop.cart.application.dto.response.WishListAddResponse;
import com.cMall.feedShop.cart.domain.exception.CartException;
import com.cMall.feedShop.cart.domain.model.WishList;
import com.cMall.feedShop.cart.domain.repository.WishlistRepository;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    @Transactional
    public WishListAddResponse addWishList(WishListRequest request, String loginId) {
        // 1. 사용자 조회
        User user = getCurrentUser(loginId);

        // 2. 상품 조회
        Product product = getProduct(request.getProductId());

        // 3. 찜 중복 검증
        validateNotAlreadyWished(user.getId(), product.getProductId());

        // 4. 찜 생성
        WishList wishlist = WishList.builder()
                .user(user)
                .product(product)
                .build();

        // 5. DB 저장
        WishList savedWishList = wishlistRepository.save(wishlist);

        // 6. 상품 찜 수 증가
        product.increaseWishNumber();

        // 7. 찜 목록 응답 생성
        return WishListAddResponse.from(savedWishList);
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

    /**
     * 주어진 상품 ID로 상품을 조회합니다.
     *
     * @param productId 조회할 상품의 ID
     * @return Product 객체
     */
    private Product getProduct(Long productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> new CartException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 사용자가 이미 찜한 상품인지 검증합니다.
     *
     * @param userId    사용자 ID
     * @param productId 상품 ID
     * @throws CartException 이미 찜한 상품인 경우
     */
    private void validateNotAlreadyWished(Long userId, Long productId) {
        if (wishlistRepository.existsActiveWishlistByUserIdAndProductId(userId, productId)) {
            throw new CartException(ErrorCode.ALREADY_WISHED_PRODUCT);
        }
    }
}
