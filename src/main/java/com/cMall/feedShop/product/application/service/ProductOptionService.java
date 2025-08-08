package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductOptionUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.info.ProductOptionInfo;
import com.cMall.feedShop.product.domain.enums.Color;
import com.cMall.feedShop.product.domain.enums.Gender;
import com.cMall.feedShop.product.domain.enums.Size;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductOptionService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 상품 ID로 상품 옵션 정보를 조회하는 서비스 메서드
     *
     * @param productId 상품 ID
     * @param userDetails 사용자 정보 (판매자)
     * @return 상품 옵션 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<ProductOptionInfo> getProductOptions(Long productId, UserDetails userDetails) {
        // 1. 현재 사용자를 가져오고 권한을 검증한다.
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한을 검증한다.
        validateSellerRole(currentUser);

        // 3. 상품 소유권을 검증하고 상품 정보를 가져온다.
        Product product = getProductOwnership(productId, currentUser.getId());

        // 4. 상품 옵션 정보를 조회하여 반환한다.
        return ProductOptionInfo.fromList(product.getProductOptions());
    }

    /**
     * 상품 옵션을 수정하는 서비스 메서드
     *
     * @param optionId 상품 옵션 ID
     * @param request  상품 옵션 수정 요청
     * @param userDetails 인증된 사용자 정보
     */
    @Transactional
    public void updateProductOption(Long optionId, ProductOptionUpdateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 정보를 가져온다.
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한을 검증한다.
        validateSellerRole(currentUser);

        // 3. 상품 옵션 정보를 가져온다.
        ProductOption productOption = getProductOption(optionId);

        // 4. 해당 상품 옵션이 내 가게 상품인지 검증한다.
        validateSellerPermission(currentUser, productOption);

        // 5. 상품 옵션 정보를 업데이트한다.
        updateOptionInfo(productOption, request);

        // 6. 변경된 상품 옵션을 저장한다.
        productOptionRepository.save(productOption);
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

    private Product getProductOwnership(Long productId, Long currentUserId) {
        // 상품을 조회한다
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 현재 사용자의 가게를 조회한다
        Store userStore = getUserStore(currentUserId);

        // 상품이 해당 가게에 속하는지 확인한다
        if (!product.getStore().getStoreId().equals(userStore.getStoreId())) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_BELONG_TO_STORE);
        }

        return product;
    }

    // 스토어 조회
    private Store getUserStore(Long userId) {
        // 내 가게를 찾는다.
        return storeRepository.findBySellerId(userId)
                .orElseThrow(() -> new ProductException(ErrorCode.STORE_NOT_FOUND));
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

    private void updateOptionInfo(ProductOption productOption, ProductOptionUpdateRequest request) {
        // 재고 업데이트 (재고는 필수값, 항상 업데이트한다.)
        productOption.updateStock(request.getStock());

        // 성별 업데이트 (성별은 선택사항, null 또는 빈 문자열이 아닐 때만 업데이트)
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                Gender gender = Gender.valueOf(request.getGender().toUpperCase());
                productOption.updateGender(gender);
            } catch (IllegalArgumentException e) {
                throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 성별 값입니다: " + request.getGender());
            }
        }

        // 사이즈 업데이트 (사이즈는 선택사항, null 또는 빈 문자열이 아닐 때만 업데이트)
        if (request.getSize() != null && !request.getSize().trim().isEmpty()) {
            try {
                Size size = Size.fromValue(request.getSize().toUpperCase());
                productOption.updateSize(size);
            } catch (IllegalArgumentException e) {
                throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 사이즈 값입니다: " + request.getSize());
            }
        }

        // 색상 업데이트 (색상은 선택사항, null 또는 빈 문자열이 아닐 때만 업데이트)
        if (request.getColor() != null && !request.getColor().trim().isEmpty()) {
            try {
                Color color = Color.valueOf(request.getColor().toUpperCase());
                productOption.updateColor(color);
            } catch (IllegalArgumentException e) {
                throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 색상 값입니다: " + request.getColor());
            }
        }
    }
}
