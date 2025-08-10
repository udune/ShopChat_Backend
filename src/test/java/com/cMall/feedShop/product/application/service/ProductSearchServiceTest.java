package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.application.calculator.DiscountCalculator;
import com.cMall.feedShop.product.application.dto.response.ProductPageResponse;
import com.cMall.feedShop.product.domain.enums.CategoryType;
import com.cMall.feedShop.product.domain.enums.DiscountType;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.store.domain.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchService 테스트")
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountCalculator discountCalculator;

    @InjectMocks
    private ProductSearchService productSearchService;

    private Product testProduct1;
    private Product testProduct2;
    private Store testStore;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 테스트용 스토어 생성
        testStore = Store.builder()
                .storeName("테스트 스토어")
                .sellerId(1L)
                .description("테스트 설명")
                .logo("http://logo.jpg")
                .build();
        ReflectionTestUtils.setField(testStore, "storeId", 1L);

        // 테스트용 카테고리 생성
        testCategory = new Category(CategoryType.SNEAKERS, "운동화");
        ReflectionTestUtils.setField(testCategory, "categoryId", 1L);

        // 테스트용 상품들 생성
        testProduct1 = Product.builder()
                .name("나이키 에어맥스")
                .price(new BigDecimal("100000"))
                .discountType(DiscountType.NONE)
                .store(testStore)
                .category(testCategory)
                .description("나이키 운동화")
                .build();
        ReflectionTestUtils.setField(testProduct1, "productId", 1L);

        testProduct2 = Product.builder()
                .name("아디다스 스탠스미스")
                .price(new BigDecimal("80000"))
                .discountType(DiscountType.NONE)
                .store(testStore)
                .category(testCategory)
                .description("아디다스 운동화")
                .build();
        ReflectionTestUtils.setField(testProduct2, "productId", 2L);
    }

    @Test
    @DisplayName("키워드로 상품 검색 성공")
    void searchProducts_WithKeyword_Success() {
        // given
        String keyword = "나이키";
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

        given(productRepository.searchProductsByName(anyString(), any(Pageable.class)))
                .willReturn(productPage);
        given(discountCalculator.calculateDiscountPrice(any(BigDecimal.class), any(), any()))
                .willReturn(new BigDecimal("100000"));

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("나이키 에어맥스");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 키워드로 전체 상품 목록 조회")
    void searchProducts_WithEmptyKeyword_ReturnsAllProducts() {
        // given
        String keyword = "";
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);

        given(productRepository.searchProductsByName(any(), any(Pageable.class)))
                .willReturn(productPage);
        given(discountCalculator.calculateDiscountPrice(any(BigDecimal.class), any(), any()))
                .willReturn(new BigDecimal("100000"));

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("null 키워드로 전체 상품 목록 조회")
    void searchProducts_WithNullKeyword_ReturnsAllProducts() {
        // given
        String keyword = null;
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);

        given(productRepository.searchProductsByName(any(), any(Pageable.class)))
                .willReturn(productPage);
        given(discountCalculator.calculateDiscountPrice(any(BigDecimal.class), any(), any()))
                .willReturn(new BigDecimal("100000"));

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 결과가 없는 경우")
    void searchProducts_NoResults_ReturnsEmptyPage() {
        // given
        String keyword = "존재하지않는상품";
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        given(productRepository.searchProductsByName(anyString(), any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지 파라미터 검증 - 음수 페이지")
    void searchProducts_NegativePage_DefaultsToZero() {
        // given
        String keyword = "나이키";
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

        given(productRepository.searchProductsByName(anyString(), any(Pageable.class)))
                .willReturn(productPage);
        given(discountCalculator.calculateDiscountPrice(any(BigDecimal.class), any(), any()))
                .willReturn(new BigDecimal("100000"));

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, -1, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지 크기 검증 - 잘못된 크기")
    void searchProducts_InvalidSize_DefaultsToTen() {
        // given
        String keyword = "나이키";
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

        given(productRepository.searchProductsByName(anyString(), any(Pageable.class)))
                .willReturn(productPage);
        given(discountCalculator.calculateDiscountPrice(any(BigDecimal.class), any(), any()))
                .willReturn(new BigDecimal("100000"));

        // when
        ProductPageResponse response = productSearchService.searchProductList(keyword, 0, 0);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(10);
    }
}