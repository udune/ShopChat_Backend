package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.application.dto.response.*;
import com.cMall.feedShop.product.application.utils.PagingUtils;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReadService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // 상품 목록 조회 (페이징)
    public ProductPageResponse getProductList(int page, int size) {
        // 페이지 정보 생성
        Pageable pageable = PagingUtils.createPageable(page, size);

        // 삭제되지 않은 상품들을 Store, 이미지와 함께 조회. (모든 상품을 페이지별로)
        Page<Product> productPage = productRepository.findAllByOrderByCreatedAtDesc(pageable);

        // 각각의 상품(Product 엔티티)을 ProductListResponse(응답값)로 변환한다.
        Page<ProductListResponse> responsePage = productPage.map(productMapper::toListResponse);

        // ProductPageResponse 에서 상품 리스트 묶어서 페이지 정보 추가. 최종 응답값 리턴
        return ProductPageResponse.of(responsePage);
    }

    // 상품 상세 조회
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // productOptions 지연 로딩 강제 초기화
        Hibernate.initialize(product.getProductOptions());

        // 상품(Product 엔티티)을 ProductDetailResponse(응답값)로 변환한다.
        return productMapper.toDetailResponse(product);
    }
}
