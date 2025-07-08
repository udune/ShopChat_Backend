package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.CategoryRepository;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    public ProductCreateResponse createProduct(ProductCreateRequest request)
    {
        // 1. 현재 사용자 ID 가져오기
        Long currentUserId = getCurrentUserId();

        // 2. 판매자 권한 검증
        validateSellerPermission(currentUserId);

        // 3. 사용자 스토어 찾기
        Store userStore = storeRepository.findBySellerId(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 4. 스토어 관리 권한 확인
        if (!userStore.isManagedBy(currentUserId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }

        // 5. 카테고리 존재 확인
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .store(userStore)
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductCreateResponse.of(savedProduct.getProductId());
    }

    // JWT 에서 현재 사용자 ID 추출 (추후 구현)
    private Long getCurrentUserId() {
        // JWT 토큰에서 사용자 ID 추출
        return 1L; // 임시
    }

    // 판매자 권한 검증 (추후 구현)
    private void validateSellerPermission(Long userId) {
        // 사용자가 ROLE_SELLER 권한을 가지고 있는지 확인
        // UserRepository 에서 사용자 조회 후 role 확인
    }
}
