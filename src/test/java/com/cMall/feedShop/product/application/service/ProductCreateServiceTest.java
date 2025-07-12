package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductImageRequest;
import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.application.exception.ProductException;
import com.cMall.feedShop.product.domain.enums.*;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCreateService 테스트")
class ProductCreateServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProductCreateService productCreateService;

    private ProductCreateRequest request;
    private User seller;
    private Store store;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = new User("seller123", "password", "seller@test.com", UserRole.SELLER);
        seller.setId(2L);

        store = Store.builder()
                .storeName("테스트 스토어")
                .sellerId(2L)
                .build();
        ReflectionTestUtils.setField(store, "storeId", 1L);

        category = new Category(CategoryType.SNEAKERS, "운동화");
        ReflectionTestUtils.setField(category, "categoryId", 1L);

        request = new ProductCreateRequest();
        ReflectionTestUtils.setField(request, "name", "테스트 상품");
        ReflectionTestUtils.setField(request, "price", new BigDecimal("50000"));
        ReflectionTestUtils.setField(request, "categoryId", 1L);
        ReflectionTestUtils.setField(request, "description", "테스트 상품입니다.");

        ProductImageRequest imageRequest = new ProductImageRequest();
        ReflectionTestUtils.setField(imageRequest, "url", "https://test.jpg");
        ReflectionTestUtils.setField(imageRequest, "type", ImageType.MAIN);
        ReflectionTestUtils.setField(request, "images", List.of(imageRequest));

        ProductOptionRequest optionRequest = new ProductOptionRequest();
        ReflectionTestUtils.setField(optionRequest, "gender", Gender.UNISEX);
        ReflectionTestUtils.setField(optionRequest, "size", Size.SIZE_250);
        ReflectionTestUtils.setField(optionRequest, "color", Color.BLACK);
        ReflectionTestUtils.setField(optionRequest, "stock", 100);
        ReflectionTestUtils.setField(request, "options", List.of(optionRequest));
    }

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
            given(authentication.getName()).willReturn("test2");

            given(userRepository.findByLoginId("test2")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(store));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(savedProduct);

            // when
            ProductCreateResponse result = productCreateService.createProduct(request);

            // then
            assertThat(result.getProductId()).isEqualTo(1L);
            verify(productRepository, times(1)).save(any(Product.class));
        }
    }

    @Test
    @DisplayName("일반 사용자가 주어졌을때_createProduct 호출하면_권한 없음 예외가 발생한다")
    void givenRegularUser_whenCreateProduct_thenThrowsForbiddenException() {
        // given
        User normalUser = new User("user123", "password", "user@test.com", UserRole.USER);
        normalUser.setId(2L);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getName()).willReturn("test2");

            given(userRepository.findByLoginId("test2")).willReturn(Optional.of(normalUser));

            // when & then
            BusinessException thrown = assertThrows(BusinessException.class, () ->
                    productCreateService.createProduct(request));

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
            given(authentication.getName()).willReturn("test2");

            given(userRepository.findByLoginId("test2")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(store));
            given(categoryRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            ProductException.CategoryNotFoundException thrown = assertThrows(
                    ProductException.CategoryNotFoundException.class, () ->
                            productCreateService.createProduct(request));

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
            given(authentication.getName()).willReturn("test2");

            given(userRepository.findByLoginId("test2")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.empty());

            // when & then
            StoreException.StoreNotFoundException thrown = assertThrows(
                    StoreException.StoreNotFoundException.class, () ->
                            productCreateService.createProduct(request));

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
                    productCreateService.createProduct(request));

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
            given(authentication.getName()).willReturn("test2");

            given(userRepository.findByLoginId("test2")).willReturn(Optional.of(seller));
            given(userRepository.findById(2L)).willReturn(Optional.of(seller));
            given(storeRepository.findBySellerId(2L)).willReturn(Optional.of(otherSellerStore));

            // when & then
            StoreException.StoreForbiddenException thrown = assertThrows(
                    StoreException.StoreForbiddenException.class, () ->
                            productCreateService.createProduct(request));

            assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.STORE_FORBIDDEN);
        }
    }
}