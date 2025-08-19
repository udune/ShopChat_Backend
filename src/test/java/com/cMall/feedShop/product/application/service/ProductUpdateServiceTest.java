package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductUpdateService 테스트")
class ProductUpdateServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductHelper productHelper;

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductUpdateService productUpdateService;

    private User sellerUser;
    private Product testProduct;
    private Category testCategory;
    private ProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        sellerUser = new User("seller", "password", "seller@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(sellerUser, "id", 1L);

        Store testStore = Store.builder()
                .storeName("테스트 스토어")
                .sellerId(1L)
                .build();
        ReflectionTestUtils.setField(testStore, "storeId", 1L);

        testCategory = new Category(null, "테스트 카테고리");
        ReflectionTestUtils.setField(testCategory, "categoryId", 1L);

        testProduct = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("10000"))
                .store(testStore)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(testProduct, "productId", 1L);

        updateRequest = new ProductUpdateRequest();
        ReflectionTestUtils.setField(updateRequest, "categoryId", 1L);
        ReflectionTestUtils.setField(updateRequest, "name", "수정된 상품");
        ReflectionTestUtils.setField(updateRequest, "price", new BigDecimal("20000"));
        ReflectionTestUtils.setField(updateRequest, "description", "수정된 설명");
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_Success() {
        // given
        given(productHelper.getCurrentUser("seller")).willReturn(sellerUser);
        given(productHelper.getProductOwnership(1L, 1L)).willReturn(testProduct);
        given(productHelper.getCategory(1L)).willReturn(testCategory);
        given(productRepository.save(any(Product.class))).willReturn(testProduct);

        // when
        productUpdateService.updateProduct(1L, updateRequest, null, null, "seller");

        // then
        verify(productHelper).getCurrentUser("seller");
        verify(productHelper).validateSellerRole(sellerUser);
        verify(productHelper).getProductOwnership(1L, 1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("상품 수정 실패 - 사용자 없음")
    void updateProduct_Fail_UserNotFound() {
        // given
        given(productHelper.getCurrentUser("nonexistent"))
                .willThrow(new ProductException(ErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> productUpdateService.updateProduct(1L, updateRequest, null, null, "nonexistent"))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("상품 수정 실패 - 상품 소유권 없음")
    void updateProduct_Fail_NoProductOwnership() {
        // given
        given(productHelper.getCurrentUser("seller")).willReturn(sellerUser);
        given(productHelper.getProductOwnership(1L, 1L))
                .willThrow(new ProductException(ErrorCode.STORE_FORBIDDEN));

        // when & then
        assertThatThrownBy(() -> productUpdateService.updateProduct(1L, updateRequest, null, null, "seller"))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_FORBIDDEN);

        verify(productRepository, never()).save(any());
    }
}