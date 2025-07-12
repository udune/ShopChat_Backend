package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductImageRequest;
import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.application.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.CategoryRepository;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.application.exception.StoreException;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // 상품 등록
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        try {
            // 1. 현재 사용자 ID 가져오기
            Long currentUserId = getCurrentUserId();

            // 2. 판매자 권한 검증
            validateSellerPermission(currentUserId);

            // 3. 사용자 스토어 찾기
            Store userStore = getUserStore(currentUserId);

            // 4. 카테고리 존재 확인
            Category category = getCategory(request.getCategoryId());

            // 5. 상품 생성
            Product product = Product.builder()
                    .name(request.getName())
                    .price(request.getPrice())
                    .description(request.getDescription())
                    .discountType(request.getDiscountType())
                    .discountValue(request.getDiscountValue())
                    .store(userStore)
                    .category(category)
                    .build();

            // 7. 상품 이미지 생성 및 저장 (메모리상에서만)
            createProductImages(product, request.getImages());

            // 8. 상품에 옵션 추가 (메모리상에서만)
            createProductOptions(product, request.getOptions());

            // 9. DB 저장
            Product savedProduct = productRepository.save(product);

            // 10. 응답값 리턴
            return ProductCreateResponse.of(savedProduct.getProductId());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 상품 수정
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        try {
            // 1. 현재 사용자 ID 가져오기
            Long currentUserId = getCurrentUserId();

            // 2. 상품 조회 (소유권 검증 포함)
            Product product = getProductOwnership(productId, currentUserId);

            // 3. 카테고리 존재 확인
            Category category = null;
            if (request.getCategoryId() != null) {
                category = getCategory(request.getCategoryId());
            }

            // 4. 상품 필드 업데이트
            updateProductFields(product, request, category);

            // 5. 이미지 업데이트
            if (request.getImages() != null) {
                updateProductImages(product, request.getImages());
            }

            // 6. 옵션 업데이트
            if (request.getOptions() != null) {
                updateProductOptions(product, request.getOptions());
            }

            // 7. DB 저장
            productRepository.save(product);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // JWT 에서 현재 사용자 ID 추출
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String login_id = authentication.getName();

        User user = userRepository.findByLoginId(login_id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return user.getId();
    }

    // 판매자 권한 검증
    private void validateSellerPermission(Long userId) {
        // 사용자가 SELLER 권한을 가지고 있는지 확인
        // UserRepository 에서 사용자 조회 후 role 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.SELLER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // 스토어 조회
    private Store getUserStore(Long userId) {
        // 내 가게를 찾는다.
        return storeRepository.findBySellerId(userId)
                .orElseThrow(() -> new StoreException.StoreNotFoundException());
    }

    // 카테고리 조회
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException.CategoryNotFoundException());
    }

    // 상품 조회 및 소유권 검증
    private Product getProductOwnership(Long productId, Long currentUserId) {
        // 상품을 찾는다.
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ProductException.ProductNotFoundException());

        // 상품 소유권 검증
        Store store = product.getStore();
        if (!store.isManagedBy(currentUserId)) {
            throw new StoreException.StoreForbiddenException();
        }

        return product;
    }

    // 상품 이미지 생성
    private void createProductImages(Product product, List<ProductImageRequest> requests)
    {
        List<ProductImage> productImages = requests.stream()
                .map(request -> new ProductImage(
                        request.getUrl(),
                        request.getType(),
                        product
                ))
                .toList();

        // Product 엔티티에 이미지 추가
        product.getProductImages().addAll(productImages);
    }

    // 상품 옵션 생성
    public void createProductOptions(Product product, List<ProductOptionRequest> requests)
    {
        List<ProductOption> productOptions = requests.stream()
                .map(request -> new ProductOption(
                        request.getGender(),
                        request.getSize(),
                        request.getColor(),
                        request.getStock(),
                        product
                ))
                .toList();

        // Product 엔티티에 옵션 추가
        product.getProductOptions().addAll(productOptions);
    }

    // 상품 필드 업데이트
    private void updateProductFields(Product product, ProductUpdateRequest request, Category category) {
        // 기본 필드 업데이트
        product.updateInfo(request.getName(), request.getPrice(), request.getDescription());

        // 할인 정보 업데이트
        product.updateDiscount(request.getDiscountType(), request.getDiscountValue());

        // 카테고리 업데이트
        product.updateCategory(category);
    }

    private void updateField(Product product, String fieldName, Object value) {
        try {
            var field = Product.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(product, value);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    private void updateProductImages(Product product, List<ProductImageRequest> requests) {
        // 기존 이미지 삭제
        product.getProductImages().clear();

        // 새로운 이미지 추가
        createProductImages(product, requests);
    }

    private void updateProductOptions(Product product, List<ProductOptionRequest> requests) {
        // 기존 옵션 삭제
        product.getProductOptions().clear();

        // 새로운 옵션 추가
        createProductOptions(product, requests);
    }
}
