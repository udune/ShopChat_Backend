package com.cMall.feedShop.cart.application.service;

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
    public void deleteWishList(Long productId, String loginId) {
        // 1. 사용자 조회
        User currentUser = getCurrentUser(loginId);

        // 2. 상품 조회
        Product product = getProduct(productId);

        // 2. 찜한 상품 조회
        WishList wishlist = wishlistRepository.findByUserIdAndProductIdAndDeletedAtIsNull(
                currentUser.getId(), productId)
                .orElseThrow(() -> new CartException(ErrorCode.WISHLIST_ITEM_NOT_FOUND));

        // 3. 찜한 상품 삭제
        wishlist.delete();

        // 4. DB 저장
        wishlistRepository.save(wishlist);

        // 5. 상품 찜 수 감소
        wishlistRepository.decreaseWishCount(productId);
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
}
