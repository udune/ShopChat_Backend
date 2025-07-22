package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductImageRequest;
import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.CategoryRepository;
import com.cMall.feedShop.product.domain.repository.ProductImageRepository;
import com.cMall.feedShop.product.domain.repository.ProductOptionRepository;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.domain.exception.StoreException;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.exception.UserException;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository productOptionRepository;

    // 상품 등록
    public ProductCreateResponse createProduct(ProductCreateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 ID 가져오기
        Long currentUserId = getCurrentUserId(userDetails);

        // 2. 판매자 권한 검증
        validateSellerPermission(currentUserId);

        // 3. 사용자 스토어 찾기
        Store userStore = getUserStore(currentUserId);

        // 4. 카테고리 존재 확인
        Category category = getCategory(request.getCategoryId());

        // 5. 상품명 중복 확인
        // (등록 시에는 아직 DB에 저장되지 않은 상태이므로
        // 현재 DB 같은 스토어에 같은 이름이 있는지만 확인)
        validateProductNameDuplication(userStore, request.getName());

        // 6. 상품 생성
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
    }

    // 상품 수정
    public void updateProduct(Long productId, ProductUpdateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 ID 가져오기
        Long currentUserId = getCurrentUserId(userDetails);

        // 2. 판매자 권한 검증
        validateSellerPermission(currentUserId);

        // 3. 상품 조회 (소유권 검증 포함)
        Product product = getProductOwnership(productId, currentUserId);

        // 4. 카테고리 존재 확인
        Category category = null;
        if (request.getCategoryId() != null) {
            category = getCategory(request.getCategoryId());
        }

        // 5. 상품명 중복 확인
        // 상품명을 변경했을 경우에만 중복 확인
        // (수정 시에는 DB에 저장된 상품과 비교하는데 자기 상품과는 비교하지 않아야 한다.)
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            validateProductNameDuplicationForUpdate(product.getStore(), request.getName(), productId);
        }

        // 5. 상품 필드 업데이트
        updateProductFields(product, request, category);

        // 6. 이미지 업데이트
        if (request.getImages() != null) {
            updateProductImages(product, request.getImages());
        }

        // 7. 옵션 업데이트
        if (request.getOptions() != null) {
            updateProductOptions(product, request.getOptions());
        }

        // 8. DB 저장
        productRepository.save(product);
    }

    // 상품 삭제
    public void deleteProduct(Long productId, UserDetails userDetails) {
        // 1. 현재 사용자 ID 가져오기
        Long currentUserId = getCurrentUserId(userDetails);

        // 2. 판매자 권한 검증
        validateSellerPermission(currentUserId);

        // 3. 상품 조회 (소유권 검증 포함)
        Product product = getProductOwnership(productId, currentUserId);

        // 4. 주문에 포함된 상품인지 확인
        validateProductNotInOrders(productId);

        // 5. DB 에서 삭제 (CASCADE DELETE)
        productRepository.delete(product);
    }

    // JWT 에서 현재 사용자 ID 추출
    private Long getCurrentUserId(UserDetails userDetails) {
        String login_id = userDetails.getUsername();
        User user = userRepository.findByLoginId(login_id)
                .orElseThrow(() -> new UserException.UserNotFoundException());

        return user.getId();
    }

    // 판매자 권한 검증
    private void validateSellerPermission(Long userId) {
        // 사용자가 SELLER 권한을 가지고 있는지 확인
        // UserRepository 에서 사용자 조회 후 role 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.UserNotFoundException());

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
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductException.ProductNotFoundException());

        // 상품 소유권 검증
        Store store = product.getStore();
        if (!store.isManagedBy(currentUserId)) {
            throw new StoreException.StoreForbiddenException();
        }

        return product;
    }

    // 주문에 포함된 상품인지 확인
    private void validateProductNotInOrders(Long productId) {
        // 주문 도메인 작업할때 진행
    }

    // 상품명 중복 확인 (상품 등록 시)
    private void validateProductNameDuplication(Store store, String productName) {
        if (productRepository.existsByStoreAndName(store, productName)) {
            throw new ProductException.DuplicateProductNameException();
        }
    }

    // 상품명 중복 확인 (상품 수정 시)
    private void validateProductNameDuplicationForUpdate(Store store, String productName, Long productId) {
        if (productRepository.existsByStoreAndNameAndProductIdNot(store, productName, productId)) {
            throw new ProductException.DuplicateProductNameException();
        }
    }

    // 상품 이미지 생성
    public void createProductImages(Product product, List<ProductImageRequest> requests)
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

    private void updateProductImages(Product product, List<ProductImageRequest> requests) {
        // 기존 이미지 삭제
        List<ProductImage> existingImages = product.getProductImages();
        if (!existingImages.isEmpty())
        {
            productImageRepository.deleteAll(existingImages);
            existingImages.clear();
        }

        // 새로운 이미지 추가
        List<ProductImage> newImages = requests.stream()
                .map(request -> new ProductImage(
                        request.getUrl(),
                        request.getType(),
                        product
                ))
                .toList();
        product.getProductImages().addAll(newImages);
    }

    private void updateProductOptions(Product product, List<ProductOptionRequest> requests) {
        // 기존 옵션 삭제
        List<ProductOption> existingOptions = product.getProductOptions();
        if (!existingOptions.isEmpty())
        {
            productOptionRepository.deleteAll(existingOptions);
            existingOptions.clear();
        }

        // 새로운 옵션 추가
        List<ProductOption> newOptions = requests.stream()
                .map(request -> new ProductOption(
                        request.getGender(),
                        request.getSize(),
                        request.getColor(),
                        request.getStock(),
                        product
                ))
                .toList();
        product.getProductOptions().addAll(newOptions);
    }
}
