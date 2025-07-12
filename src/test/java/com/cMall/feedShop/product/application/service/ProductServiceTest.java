package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductImageRequest;
import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.application.exception.ProductException;
import com.cMall.feedShop.product.domain.enums.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    private ProductCreateRequest createRequest;
    private ProductUpdateRequest updateRequest;
    private User seller;
    private User otherSeller;
    private User regularUser;
    private Store store;
    private Store otherStore;
    private Category category;
    private Category newCategory;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        // 사용자 설정
        seller = new User("seller123", "password", "seller@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(seller, "id", 2L);

        otherSeller = new User("other123", "password", "other@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(otherSeller, "id", 3L);

        regularUser = new User("user123", "password", "user@test.com", UserRole.USER);
        ReflectionTestUtils.setField(regularUser, "id", 4L);

        // 스토어 설정
        store = Store.builder()
                .storeName("테스트 스토어")
                .sellerId(2L)
                .build();
        ReflectionTestUtils.setField(store, "storeId", 1L);

        otherStore = Store.builder()
                .storeName("다른 스토어")
                .sellerId(3L)
                .build();
        ReflectionTestUtils.setField(otherStore, "storeId", 2L);

        // 카테고리 설정
        category = new Category(CategoryType.SNEAKERS, "운동화");
        ReflectionTestUtils.setField(category, "categoryId", 1L);

        newCategory = new Category(CategoryType.BOOTS, "부츠");
        ReflectionTestUtils.setField(newCategory, "categoryId", 2L);

        // 기존 상품 설정 (수정용)
        existingProduct = Product.builder()
                .name("기존 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .description("기존 설명")
                .build();
        ReflectionTestUtils.setField(existingProduct, "productId", 1L);
        ReflectionTestUtils.setField(existingProduct, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(existingProduct, "updatedAt", LocalDateTime.now());

        // 기존 이미지와 옵션 설정
        List<ProductImage> existingImages = new ArrayList<>();
        existingImages.add(new ProductImage("http://old.jpg", ImageType.MAIN, existingProduct));
        ReflectionTestUtils.setField(existingProduct, "productImages", existingImages);

        List<ProductOption> existingOptions = new ArrayList<>();
        existingOptions.add(new ProductOption(Gender.MEN, Size.SIZE_250, Color.BLACK, 10, existingProduct));
        ReflectionTestUtils.setField(existingProduct, "productOptions", existingOptions);

        // 생성 요청 설정
        createRequest = new ProductCreateRequest();
        ReflectionTestUtils.setField(createRequest, "name", "테스트 상품");
        ReflectionTestUtils.setField(createRequest, "price", new BigDecimal("50000"));
        ReflectionTestUtils.setField(createRequest, "categoryId", 1L);
        ReflectionTestUtils.setField(createRequest, "description", "테스트 상품입니다.");

        ProductImageRequest imageRequest = new ProductImageRequest();
        ReflectionTestUtils.setField(imageRequest, "url", "https://test.jpg");
        ReflectionTestUtils.setField(imageRequest, "type", ImageType.MAIN);
        ReflectionTestUtils.setField(createRequest, "images", List.of(imageRequest));

        ProductOptionRequest optionRequest = new ProductOptionRequest();
        ReflectionTestUtils.setField(optionRequest, "gender", Gender.UNISEX);
        ReflectionTestUtils.setField(optionRequest, "size", Size.SIZE_250);
        ReflectionTestUtils.setField(optionRequest, "color", Color.BLACK);
        ReflectionTestUtils.setField(optionRequest, "stock", 100);
        ReflectionTestUtils.setField(createRequest, "options", List.of(optionRequest));

        // 수정 요청 설정
        updateRequest = new ProductUpdateRequest();
    }

    // ================================
    // createProduct 테스트 (기존)
    // ================================

    @Test
    @DisplayName("유효한 판매자와 요청이 주어졌을때_createProduct 호출하면_상품이 성공적으로 생성된다")
    void givenValidSellerAndRequest_whenCreateProduct_thenProductCreatedSuccessfully() {
        // given
        Product savedProduct = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();
        ReflectionTestUtils.setField(savedProduct, "productId", 1L);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(store));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(savedProduct);

            // when
            ProductCreateResponse result = productService.createProduct(createRequest);

            // then
            assertThat(result.getProductId()).isEqualTo(1L);
            verify(productRepository, times(1)).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("일반 사용자가 주어졌을때_createProduct 호출하면_권한 없음 예외가 발생한다")
    void givenRegularUser_whenCreateProduct_thenThrowsForbiddenException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("user123");

            given(userRepository.findByLoginId("user123")).willReturn(Optional.of(regularUser));

            // when & then
            BusinessException thrown = assertThrows(BusinessException.class, () ->
                    productService.createProduct(createRequest));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("존재하지않는 카테고리ID가 주어졌을때_createProduct 호출하면_카테고리 없음 예외가 발생한다")
    void givenNonExistentCategoryId_whenCreateProduct_thenThrowsCategoryNotFoundException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(store));
            given(categoryRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            ProductException.CategoryNotFoundException thrown = assertThrows(
                    ProductException.CategoryNotFoundException.class, () ->
                            productService.createProduct(createRequest));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("스토어가 존재하지않는 판매자가 주어졌을때_createProduct 호출하면_스토어 없음 예외가 발생한다")
    void givenSellerWithoutStore_whenCreateProduct_thenThrowsStoreNotFoundException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.empty());

            // when & then
            StoreException.StoreNotFoundException thrown = assertThrows(
                    StoreException.StoreNotFoundException.class, () ->
                            productService.createProduct(createRequest));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("인증되지않은 사용자가 주어졌을때_createProduct 호출하면_인증 없음 예외가 발생한다")
    void givenUnauthenticatedUser_whenCreateProduct_thenThrowsUnauthorizedException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(null);

            // when & then
            BusinessException thrown = assertThrows(BusinessException.class, () ->
                    productService.createProduct(createRequest));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("다른 판매자의 스토어에 상품을 등록하려할때_createProduct 호출하면_권한 없음 예외가 발생한다")
    void givenSellerTryingToUseOtherStore_whenCreateProduct_thenThrowsStoreForbiddenException() {
        // given
        Store otherSellerStore = Store.builder()
                .storeName("다른 판매자 스토어")
                .sellerId(999L) // 다른 판매자 ID
                .build();
        ReflectionTestUtils.setField(otherSellerStore, "storeId", 1L);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(otherSellerStore));

            // when & then
            StoreException.StoreForbiddenException thrown = assertThrows(
                    StoreException.StoreForbiddenException.class, () ->
                            productService.createProduct(createRequest));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.STORE_FORBIDDEN);
        }
    }

    // ================================
    // updateProduct 테스트 (신규 추가)
    // ================================

    @Test
    @DisplayName("유효한 판매자와 요청이 주어졌을때_updateProduct 호출하면_상품이 성공적으로 수정된다")
    void givenValidSellerAndRequest_whenUpdateProduct_thenProductUpdatedSuccessfully() {
        // given
        ReflectionTestUtils.setField(updateRequest, "name", "수정된 상품명");
        ReflectionTestUtils.setField(updateRequest, "price", new BigDecimal("60000"));
        ReflectionTestUtils.setField(updateRequest, "categoryId", 2L);
        ReflectionTestUtils.setField(updateRequest, "description", "수정된 설명");

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(existingProduct));
            given(categoryRepository.findById(2L)).willReturn(Optional.of(newCategory));
            given(productRepository.save(any(Product.class))).willReturn(existingProduct);

            // when
            productService.updateProduct(1L, updateRequest);

            // then
            verify(productRepository, times(1)).save(any(Product.class));
            verify(categoryRepository, times(1)).findById(2L);
        }
    }

    @Test
    @DisplayName("부분 업데이트가 주어졌을때_updateProduct 호출하면_해당 필드만 수정된다")
    void givenPartialUpdate_whenUpdateProduct_thenOnlySpecifiedFieldsUpdated() {
        // given - 이름만 수정하는 부분 업데이트
        ReflectionTestUtils.setField(updateRequest, "name", "부분 수정된 이름");
        // price, description 등은 null

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(existingProduct));
            given(productRepository.save(any(Product.class))).willReturn(existingProduct);

            // when
            productService.updateProduct(1L, updateRequest);

            // then
            verify(productRepository, times(1)).save(any(Product.class));
            // 카테고리 조회는 호출되지 않아야 함
            verify(categoryRepository, never()).findById(any());
        }
    }

    @Test
    @DisplayName("이미지 업데이트가 주어졌을때_updateProduct 호출하면_이미지가 교체된다")
    void givenImageUpdate_whenUpdateProduct_thenImagesReplaced() {
        // given
        ProductImageRequest newImageRequest = new ProductImageRequest();
        ReflectionTestUtils.setField(newImageRequest, "url", "http://new.jpg");
        ReflectionTestUtils.setField(newImageRequest, "type", ImageType.MAIN);
        ReflectionTestUtils.setField(updateRequest, "images", List.of(newImageRequest));

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(existingProduct));
            given(productRepository.save(any(Product.class))).willReturn(existingProduct);

            // when
            productService.updateProduct(1L, updateRequest);

            // then
            verify(productRepository, times(1)).save(any(Product.class));
            assertThat(existingProduct.getProductImages()).hasSize(1);
        }
    }

    @Test
    @DisplayName("다른 판매자의 상품을 수정하려할때_updateProduct 호출하면_권한 없음 예외가 발생한다")
    void givenOtherSellerProduct_whenUpdateProduct_thenThrowsStoreForbiddenException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("other123");

            given(userRepository.findByLoginId("other123")).willReturn(Optional.of(otherSeller));
            given(userRepository.findById(3L)).willReturn(Optional.of(otherSeller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(existingProduct));

            // when & then
            StoreException.StoreForbiddenException thrown = assertThrows(
                    StoreException.StoreForbiddenException.class,
                    () -> productService.updateProduct(1L, updateRequest)
            );

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.STORE_FORBIDDEN);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("일반 사용자가 상품을 수정하려할때_updateProduct 호출하면_권한 없음 예외가 발생한다")
    void givenRegularUser_whenUpdateProduct_thenThrowsForbiddenException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("user123");

            given(userRepository.findByLoginId("user123")).willReturn(Optional.of(regularUser));

            // when & then
            BusinessException thrown = assertThrows(
                    BusinessException.class,
                    () -> productService.updateProduct(1L, updateRequest)
            );

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("존재하지않는 상품을 수정하려할때_updateProduct 호출하면_상품 없음 예외가 발생한다")
    void givenNonExistentProduct_whenUpdateProduct_thenThrowsProductNotFoundException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            // when & then
            ProductException.ProductNotFoundException thrown = assertThrows(
                    ProductException.ProductNotFoundException.class,
                    () -> productService.updateProduct(999L, updateRequest)
            );

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("존재하지않는 카테고리로 수정하려할때_updateProduct 호출하면_카테고리 없음 예외가 발생한다")
    void givenNonExistentCategory_whenUpdateProduct_thenThrowsCategoryNotFoundException() {
        // given
        ReflectionTestUtils.setField(updateRequest, "categoryId", 999L);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("seller123");

            given(userRepository.findByLoginId("seller123")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(productRepository.findByProductIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(existingProduct));
            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            ProductException.CategoryNotFoundException thrown = assertThrows(
                    ProductException.CategoryNotFoundException.class,
                    () -> productService.updateProduct(1L, updateRequest)
            );

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("인증되지않은 사용자가 상품을 수정하려할때_updateProduct 호출하면_인증 없음 예외가 발생한다")
    void givenUnauthenticatedUser_whenUpdateProduct_thenThrowsUnauthorizedException() {
        // given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(null);

            // when & then
            BusinessException thrown = assertThrows(
                    BusinessException.class,
                    () -> productService.updateProduct(1L, updateRequest)
            );

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(productRepository, never()).save(any(Product.class));
        }
    }
}