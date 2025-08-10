package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.application.calculator.DiscountCalculator;
import com.cMall.feedShop.product.application.dto.response.ProductListResponse;
import com.cMall.feedShop.product.application.dto.response.ProductPageResponse;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final DiscountCalculator discountCalculator;

    /**
     * 상품 목록 검색 서비스
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 상품 목록 응답
     */
    public ProductPageResponse searchProductList(String keyword, int page, int size) {
        // 페이지 정보 생성
        Pageable pageable = createPageable(page, size);

        // 상품 검색 (키워드가 있으면 검색, 없으면 전체 목록)
        Page<Product> productPage = productRepository.searchProductsByName(keyword, pageable);

        // 각각의 상품(Product 엔티티)을 ProductListResponse(응답값)로 변환한다.
        Page<ProductListResponse> responsePage = productPage.map(this::convertToProductListResponse);

        // ProductPageResponse 에서 상품 리스트 묶어서 페이지 정보 추가. 최종 응답값 리턴
        return ProductPageResponse.of(responsePage);
    }

    private Pageable createPageable(int page, int size) {
        page = Math.max(page, 0); // 페이지 번호는 0 이상
        size = (size < 1 || size > 100) ? 20 : size; // 기본값 20, 최대 100
        return PageRequest.of(page, size);
    }

    // 각각의 상품들을 ProductListResponse로 변환한다.
    private ProductListResponse convertToProductListResponse(Product product) {

        // 할인가를 계산한다.
        BigDecimal discountPrice = product.getDiscountPrice(discountCalculator);

        // 대표 이미지 URL 가져오기
        String mainImageUrl = product.getMainImageUrl();

        // ProductListResponse 에서 응답값(상품 정보)을 생성해준다.
        return ProductListResponse.of(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                discountPrice,
                product.getCategory().getCategoryId(),
                product.getStore().getStoreId(),
                product.getStore().getStoreName(),
                product.getWishNumber(),
                mainImageUrl
        );
    }
}
