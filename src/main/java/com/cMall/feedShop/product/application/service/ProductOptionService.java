package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductOptionUpdateRequest;
import com.cMall.feedShop.product.domain.enums.Gender;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.ProductOptionRepository;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
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
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 상품 옵션을 수정하는 서비스 메서드
     *
     * @param optionId 상품 옵션 ID
     * @param request  상품 옵션 수정 요청
     * @param userDetails 인증된 사용자 정보
     */
    @Transactional
    public void updateProductOption(Long optionId, ProductOptionUpdateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 ID를 가져온다.
        Long currentUserId = getCurrentUserId(userDetails);

        // 2. 상품 옵션 정보를 가져온다.
        ProductOption productOption = getProductOption(optionId);

        updateOptionInfo(productOption, request);
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        // 사용자 정보에서 로그인 ID를 가져온다.
        String loginId = userDetails.getUsername();

        // 로그인 ID로 사용자 정보를 조회한다.
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 SELLER 권한을 가지고 있는지 확인
        if (user.getRole() != UserRole.SELLER) {
            throw new ProductException(ErrorCode.FORBIDDEN);
        }

        // 사용자 ID를 반환한다.
        return user.getId();
    }

    private ProductOption getProductOption(Long optionId) {
        return productOptionRepository.findByOptionId(optionId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
    }

    private void updateOptionInfo(ProductOption productOption, ProductOptionUpdateRequest request) {
        // 재고 업데이트 (재고는 필수값, 항상 업데이트한다.)
        productOption.updateStock(request.getStock());

        // 성별 업데이트 (성별은 선택사항, null 또는 빈 문자열이 아닐 때만 업데이트)
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                Gender gender = Gender.valueOf(request.getGender().toUpperCase());
                productOption.updateGender(gender);
            } catch (IllegalArgumentException e) {
                throw new ProductException(ErrorCode.INVALID_G
        }
    }
}
