package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.application.dto.request.ProductOptionRequest;
import com.cMall.feedShop.product.domain.enums.*;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.store.domain.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 통합 테스트")
class ProductServiceIntegrationTest {

    @Mock
    private ProductImageService productImageService;

    private ProductService productService;
    private Store store;
    private Category category;

    @BeforeEach
    void setUp() {
        // ProductImageService만 Mock으로 주입하고 나머지는 null
        productService = new ProductService(
                null, null, null, null, null, null, productImageService
        );

        store = Store.builder()
                .storeName("테스트 스토어")
                .sellerId(1L)
                .build();
        ReflectionTestUtils.setField(store, "storeId", 1L);

        category = new Category(CategoryType.SNEAKERS, "운동화");
        ReflectionTestUtils.setField(category, "categoryId", 1L);
    }

    @Test
    @DisplayName("상품 이미지 생성 메서드 테스트 - MAIN 타입")
    void addImages_MainType_Success() throws Exception {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        List<MultipartFile> mainImages = Arrays.asList(
                new MockMultipartFile("mainImage1", "main1.jpg", "image/jpeg", "main image 1".getBytes()),
                new MockMultipartFile("mainImage2", "main2.jpg", "image/jpeg", "main image 2".getBytes())
        );

        // Mock ProductImage 객체들 생성
        List<ProductImage> mockProductImages = Arrays.asList(
                new ProductImage("main1.jpg", ImageType.MAIN, product),
                new ProductImage("main2.jpg", ImageType.MAIN, product)
        );

        // ProductImageService Mock 설정
        given(productImageService.uploadImages(eq(product), eq(mainImages), eq(ImageType.MAIN)))
                .willReturn(mockProductImages);

        // when - 리플렉션을 사용하여 private 메서드 호출
        java.lang.reflect.Method method = ProductService.class
                .getDeclaredMethod("addImages", Product.class, List.class, ImageType.class);
        method.setAccessible(true);
        method.invoke(productService, product, mainImages, ImageType.MAIN);

        // then
        assertThat(product.getProductImages()).hasSize(2);
        assertThat(product.getProductImages().get(0).getUrl()).isEqualTo("main1.jpg");
        assertThat(product.getProductImages().get(0).getType()).isEqualTo(ImageType.MAIN);
        assertThat(product.getProductImages().get(1).getUrl()).isEqualTo("main2.jpg");
        assertThat(product.getProductImages().get(1).getType()).isEqualTo(ImageType.MAIN);
    }

    @Test
    @DisplayName("상품 이미지 생성 메서드 테스트 - DETAIL 타입")
    void addImages_DetailType_Success() throws Exception {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        List<MultipartFile> detailImages = Arrays.asList(
                new MockMultipartFile("detailImage", "detail.jpg", "image/jpeg", "detail image".getBytes())
        );

        List<ProductImage> mockProductImages = Arrays.asList(
                new ProductImage("detail.jpg", ImageType.DETAIL, product)
        );

        given(productImageService.uploadImages(eq(product), eq(detailImages), eq(ImageType.DETAIL)))
                .willReturn(mockProductImages);

        // when
        java.lang.reflect.Method method = ProductService.class
                .getDeclaredMethod("addImages", Product.class, List.class, ImageType.class);
        method.setAccessible(true);
        method.invoke(productService, product, detailImages, ImageType.DETAIL);

        // then
        assertThat(product.getProductImages()).hasSize(1);
        assertThat(product.getProductImages().get(0).getUrl()).isEqualTo("detail.jpg");
        assertThat(product.getProductImages().get(0).getType()).isEqualTo(ImageType.DETAIL);
    }

    @Test
    @DisplayName("상품 옵션 생성 메서드 테스트")
    void addOptions_Success() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        ProductOptionRequest optionRequest1 = new ProductOptionRequest();
        ReflectionTestUtils.setField(optionRequest1, "gender", Gender.MEN);
        ReflectionTestUtils.setField(optionRequest1, "size", Size.SIZE_270);
        ReflectionTestUtils.setField(optionRequest1, "color", Color.BLACK);
        ReflectionTestUtils.setField(optionRequest1, "stock", 50);

        ProductOptionRequest optionRequest2 = new ProductOptionRequest();
        ReflectionTestUtils.setField(optionRequest2, "gender", Gender.WOMEN);
        ReflectionTestUtils.setField(optionRequest2, "size", Size.SIZE_240);
        ReflectionTestUtils.setField(optionRequest2, "color", Color.WHITE);
        ReflectionTestUtils.setField(optionRequest2, "stock", 30);

        List<ProductOptionRequest> requests = Arrays.asList(optionRequest1, optionRequest2);

        // when
        productService.addOptions(product, requests);

        // then
        assertThat(product.getProductOptions()).hasSize(2);
        assertThat(product.getProductOptions().get(0).getGender()).isEqualTo(Gender.MEN);
        assertThat(product.getProductOptions().get(0).getSize()).isEqualTo(Size.SIZE_270);
        assertThat(product.getProductOptions().get(0).getColor()).isEqualTo(Color.BLACK);
        assertThat(product.getProductOptions().get(0).getStock()).isEqualTo(50);

        assertThat(product.getProductOptions().get(1).getGender()).isEqualTo(Gender.WOMEN);
        assertThat(product.getProductOptions().get(1).getSize()).isEqualTo(Size.SIZE_240);
        assertThat(product.getProductOptions().get(1).getColor()).isEqualTo(Color.WHITE);
        assertThat(product.getProductOptions().get(1).getStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("빈 이미지 리스트로 상품 이미지 생성")
    void addImages_EmptyList() throws Exception {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        List<MultipartFile> emptyImages = List.of();

        // Mock 설정 불필요 - uploadImages 메서드 내부에서 빈 리스트는 바로 리턴

        // when
        java.lang.reflect.Method method = ProductService.class
                .getDeclaredMethod("addImages", Product.class, List.class, ImageType.class);
        method.setAccessible(true);
        method.invoke(productService, product, emptyImages, ImageType.MAIN);

        // then
        assertThat(product.getProductImages()).isEmpty();
    }

    @Test
    @DisplayName("null 이미지 리스트로 상품 이미지 생성")
    void addImages_NullList() throws Exception {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        // Mock 설정 불필요 - uploadImages 메서드 내부에서 null은 바로 리턴

        // when
        java.lang.reflect.Method method = ProductService.class
                .getDeclaredMethod("addImages", Product.class, List.class, ImageType.class);
        method.setAccessible(true);
        method.invoke(productService, product, null, ImageType.MAIN);

        // then
        assertThat(product.getProductImages()).isEmpty();
    }

    @Test
    @DisplayName("빈 옵션 리스트로 상품 옵션 생성")
    void addOptions_EmptyList() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        List<ProductOptionRequest> emptyRequests = List.of();

        // when
        productService.addOptions(product, emptyRequests);

        // then
        assertThat(product.getProductOptions()).isEmpty();
    }

    @Test
    @DisplayName("null 옵션 리스트로 상품 옵션 생성")
    void addOptions_NullList() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        // when
        productService.addOptions(product, null);

        // then
        assertThat(product.getProductOptions()).isEmpty();
    }

    @Test
    @DisplayName("여러 타입의 이미지를 동시에 추가")
    void addImages_MultipleTypes() throws Exception {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("50000"))
                .store(store)
                .category(category)
                .build();

        List<MultipartFile> mainImages = Arrays.asList(
                new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "main image".getBytes())
        );

        List<MultipartFile> detailImages = Arrays.asList(
                new MockMultipartFile("detailImage", "detail.jpg", "image/jpeg", "detail image".getBytes())
        );

        List<ProductImage> mockMainImages = Arrays.asList(
                new ProductImage("main.jpg", ImageType.MAIN, product)
        );

        List<ProductImage> mockDetailImages = Arrays.asList(
                new ProductImage("detail.jpg", ImageType.DETAIL, product)
        );

        given(productImageService.uploadImages(eq(product), eq(mainImages), eq(ImageType.MAIN)))
                .willReturn(mockMainImages);
        given(productImageService.uploadImages(eq(product), eq(detailImages), eq(ImageType.DETAIL)))
                .willReturn(mockDetailImages);

        java.lang.reflect.Method method = ProductService.class
                .getDeclaredMethod("addImages", Product.class, List.class, ImageType.class);
        method.setAccessible(true);

        // when
        method.invoke(productService, product, mainImages, ImageType.MAIN);
        method.invoke(productService, product, detailImages, ImageType.DETAIL);

        // then
        assertThat(product.getProductImages()).hasSize(2);
        assertThat(product.getProductImages().get(0).getType()).isEqualTo(ImageType.MAIN);
        assertThat(product.getProductImages().get(1).getType()).isEqualTo(ImageType.DETAIL);
    }
}