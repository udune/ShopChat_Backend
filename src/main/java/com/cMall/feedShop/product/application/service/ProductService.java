package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.common.service.GcpStorageService;
import com.cMall.feedShop.order.domain.repository.OrderItemRepository;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductImageRequest;
import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.CategoryRepository;
import com.cMall.feedShop.product.domain.repository.ProductImageRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    private final OrderItemRepository orderItemRepository;
    private final GcpStorageService gcpStorageService;

    // 상품 등록
    public ProductCreateResponse createProduct(ProductCreateRequest request, List<MultipartFile> mainImages, List<MultipartFile> detailImages, UserDetails userDetails) {
        // 1. 현재 사용자 정보 가져오기 및 권한 검증
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한 검증
        validateSellerRole(currentUser);

        // 3. 사용자 스토어 찾기
        Store userStore = getUserStore(currentUser.getId());

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

        // 7. 이미지 생성

        // 메인 이미지
        if (mainImages != null && !mainImages.isEmpty()) {
            createProductImages(product, mainImages, ImageType.MAIN);
        }

        // 상세 이미지
        if (detailImages != null && !detailImages.isEmpty()) {
            createProductImages(product, detailImages, ImageType.DETAIL);
        }

        // 8. 상품에 옵션 추가 (메모리상에서만)
        createProductOptions(product, request.getOptions());

        // 9. DB 저장
        Product savedProduct = productRepository.save(product);

        // 10. 응답값 리턴
        return ProductCreateResponse.of(savedProduct.getProductId());
    }

    // 상품 수정
    public void updateProduct(Long productId, ProductUpdateRequest request, List<MultipartFile> mainImages, List<MultipartFile> detailImages, UserDetails userDetails) {
        // 1. 현재 사용자 정보 가져오기 및 권한 검증
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한 검증
        validateSellerRole(currentUser);

        // 3. 상품 조회 (소유권 검증 포함)
        Product product = getProductOwnership(productId, currentUser.getId());

        // 4. 카테고리 존재 확인
        Category category = null;
        if (request.getCategoryId() != null) {
            category = getCategory(request.getCategoryId());
        }

        // 5. 상품명 중복 확인
        // 상품명을 변경했을 경우에만 중복 확인
        // (수정 시에는 DB에 저장된 상품과 비교하는데 자기 상품과는 비교하지 않아야 한다.)
        String currentName = product.getName();
        String newName = request.getName();
        boolean isNameChanged = newName != null && !newName.equals(currentName);
        if (isNameChanged) {
            validateProductNameDuplicationForUpdate(product.getStore(), newName, productId);
        }

        // 6. 상품 필드 업데이트
        updateProductFields(product, request, category);

        // 7. 이미지 업데이트

        // 메인 이미지
        if (mainImages != null && !mainImages.isEmpty()) {
            updateProductImages(product, mainImages, ImageType.MAIN);
        }

        // 상세 이미지
        if (detailImages != null && !detailImages.isEmpty()) {
            updateProductImages(product, detailImages, ImageType.DETAIL);
        }

        // 8. 옵션 업데이트
        if (request.getOptions() != null) {
            updateProductOptions(product, request.getOptions());
        }

        // 9. DB 저장
        productRepository.save(product);
    }

    // 상품 삭제
    public void deleteProduct(Long productId, UserDetails userDetails) {
        // 1. 현재 사용자 정보 가져오기 및 권한 검증
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한 검증
        validateSellerRole(currentUser);

        // 3. 상품 조회 (소유권 검증 포함)
        Product product = getProductOwnership(productId, currentUser.getId());

        // 4. 주문에 포함된 상품인지 확인
        validateProductNotInOrder(product);

        // 5. DB 에서 삭제 (CASCADE DELETE)
        productRepository.delete(product);
    }

    // JWT 에서 현재 사용자 추출
    private User getCurrentUser(UserDetails userDetails) {
        String loginId = userDetails.getUsername();
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND));
    }

    // 스토어 조회
    private Store getUserStore(Long userId) {
        // 내 가게를 찾는다.
        return storeRepository.findBySellerId(userId)
                .orElseThrow(() -> new ProductException(ErrorCode.STORE_NOT_FOUND));
    }

    // 카테고리 조회
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // 상품 조회 및 소유권 검증
    private Product getProductOwnership(Long productId, Long currentUserId) {
        // 상품을 찾는다.
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 상품 소유권 검증
        Store store = product.getStore();
        if (!store.isManagedBy(currentUserId)) {
            throw new ProductException(ErrorCode.STORE_FORBIDDEN);
        }

        return product;
    }

    // 판매자 권한을 검증한다.
    private void validateSellerRole(User user) {
        if (user.getRole() != UserRole.SELLER) {
            throw new ProductException(ErrorCode.FORBIDDEN);
        }
    }

    // 상품명 중복 확인 (상품 등록 시)
    private void validateProductNameDuplication(Store store, String productName) {
        if (productRepository.existsByStoreAndName(store, productName)) {
            throw new ProductException(ErrorCode.DUPLICATE_PRODUCT_NAME);
        }
    }

    // 상품명 중복 확인 (상품 수정 시)
    private void validateProductNameDuplicationForUpdate(Store store, String productName, Long productId) {
        if (productRepository.existsByStoreAndNameAndProductIdNot(store, productName, productId)) {
            throw new ProductException(ErrorCode.DUPLICATE_PRODUCT_NAME);
        }
    }

    // 주문에 포함된 상품인지 확인 (추후 리팩토링으로 soft delete로 변경 예정)
    private void validateProductNotInOrder(Product product) {
        // 해당 상품의 모든 옵션들을 가져와서 주문에 포함되었는지 확인
        List<ProductOption> productOptions = product.getProductOptions();

        // 상품 옵션들 중 하나라도 주문에 포함되어 있다면 삭제 불가
        for (ProductOption option : productOptions) {
            if (orderItemRepository.existsByProductOption(option)) {
                throw new ProductException(ErrorCode.PRODUCT_IN_ORDER);
            }
        }
    }

    // 상품 이미지 생성
    public void createProductImages(Product product, List<MultipartFile> images, ImageType type) {
        if (images == null || images.isEmpty()) {
            return; // 이미지가 없으면 아무 작업도 하지 않음
        }

        try {
            List<GcpStorageService.UploadResult> uploadResults = gcpStorageService.uploadFilesWithDetails(images, "products");

            if (!uploadResults.isEmpty()) {
                List<ProductImage> productImages = new ArrayList<>();

                for (GcpStorageService.UploadResult uploadResult : uploadResults) {
                    // 업로드된 이미지 URL 에서 상대 경로 추출
                    String imageUrl = gcpStorageService.extractObjectName(uploadResult.getFilePath());

                    // ProductImage 엔티티 생성
                    ProductImage productImage = new ProductImage(
                            imageUrl,
                            type,
                            product
                    );

                    // productImages 리스트에 추가
                    productImages.add(productImage);
                }

                // Product 엔티티에 이미지 추가
                product.getProductImages().addAll(productImages);
            }
        } catch (Exception e) {
            throw new ProductException(ErrorCode.FILE_UPLOAD_ERROR, "이미지 업로드에 실패했습니다: " + e.getMessage());
        }
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

    private void updateProductImages(Product product, List<MultipartFile> images, ImageType type) {
        // 기존 이미지 삭제
        List<ProductImage> existingImages = product.getProductImages().stream()
                .filter(image -> image.getType() == type)
                .toList();
        if (!existingImages.isEmpty())
        {
            // GCP Storage 에서 이미지 파일 삭제
            for (ProductImage image : existingImages) {
                try {
                    gcpStorageService.deleteFile(gcpStorageService.getFullFilePath(image.getUrl()));
                } catch (Exception e) {
                    throw new ProductException(ErrorCode.FILE_DELETE_ERROR, "이미지 삭제에 실패했습니다: " + e.getMessage());
                }
            }

            // ProductImage 엔티티에서 삭제
            product.getProductImages().removeAll(existingImages);
            productImageRepository.deleteAll(existingImages);
        }

        // 새로운 이미지 추가
        createProductImages(product, images, type);
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