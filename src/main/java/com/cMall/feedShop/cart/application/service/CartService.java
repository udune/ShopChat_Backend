package com.cMall.feedShop.cart.application.service;

import com.cMall.feedShop.cart.application.dto.request.CartItemCreateRequest;
import com.cMall.feedShop.cart.application.dto.response.CartItemResponse;
import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.ProductImageRepository;
import com.cMall.feedShop.product.domain.repository.ProductOptionRepository;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductImageRepository productImageRepository;

    public CartItemResponse addCartItem(CartItemCreateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 ID 가져오기
        Long currentUserId = getCurrentUserId(userDetails);

        // 2. 상품 옵션과 이미지 검증
        ProductOption productOption = validateProductOption(request.getOptionId());
        ProductImage productImage = validateProductImage(request.getImageId());

        // 3. 재고 확인
        validateStock(productOption, request.getQuantity());
    }

    // JWT 에서 현재 사용자 ID 추출
    private Long getCurrentUserId(UserDetails userDetails) {
        String login_id = userDetails.getUsername();
        User user = userRepository.findByLoginId(login_id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return user.getId();
    }

    private ProductOption validateProductOption(Long optionId) {
        // ProductOption을 찾는다.
        ProductOption productOption = productOptionRepository.findByOptionId(optionId)
                .orElseThrow(() -> new ProductException.ProductOptionNotFoundException());

        return productOption;
    }

    private ProductImage validateProductImage(Long imageId) {
        // ProductImage를 찾는다.
        ProductImage productImage = productImageRepository.findByImageId(imageId)
                .orElseThrow(() -> new ProductException.ProductImageNotFoundException());
        return productImage;
    }

    private void validateStock(ProductOption productOption, Integer quantity) {
        if (!productOption.isInStock()) {
            throw new ProductException.ProductOutOfStockException();
        }

        if (productOption.getStock() < quantity) {
            throw new ProductException.ProductOutOfStockException("재고가 부족합니다. 현재 재고: " + productOption.getStock());
        }
    }
}
