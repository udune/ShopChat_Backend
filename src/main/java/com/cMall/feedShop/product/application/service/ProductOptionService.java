package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.ProductOptionRepository;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductOptionService {

    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;

    @Transactional
    public void deleteProductOption(Long optionId, UserDetails userDetails) {
        // 1. 현재 사용자 정보를 가져온다.
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한을 검증한다.
        validateSellerRole(currentUser);

        // 3. 상품 옵션 정보를 가져온다.
        ProductOption productOption = getProductOption(optionId);

        // 4. 해당 상품 옵션이 내 가게 상품인지 검증한다.
        validateSellerPermission(currentUser, productOption);

        // 5. 주문 내역이 있는지 확인한다.
        validateNotOrderedOption(productOption);

        // 6. 상품 옵션을 삭제한다.
        productOptionRepository.delete(productOption);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND));
    }

    // 판매자 권한을 검증한다.
    private void validateSellerRole(User user) {
        if (user.getRole() != UserRole.SELLER) {
            throw new ProductException(ErrorCode.FORBIDDEN);
        }
    }

    private ProductOption getProductOption(Long optionId) {
        return productOptionRepository.findByOptionId(optionId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
    }

    private void validateSellerPermission(User currentUser, ProductOption productOption) {
        Long sellerId = productOption.getProduct().getStore().getSellerId();

        if (!currentUser.getId().equals(sellerId)) {
            throw new ProductException(ErrorCode.FORBIDDEN, "해당 상품 옵션에 대한 권한이 없습니다.");
        }
    }

    // 추후 리팩토링으로 soft delete로 변경할 예정.
    private void validateNotOrderedOption(ProductOption productOption) {
        if (!productOption.getOrderItems().isEmpty()) {
            throw new ProductException(ErrorCode.PRODUCT_IN_ORDER);
        }
    }
}
