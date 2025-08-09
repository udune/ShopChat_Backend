package com.cMall.feedShop.product.infrastructure.repository;

import com.cMall.feedShop.product.domain.model.ProductImage;

import java.util.List;
import java.util.Set;

public interface ProductImageQueryRepository {

    // 여러 상품의 메인 이미지들을 한 번에 조회한다.
    List<ProductImage> findMainImagesByProductIds(Set<Long> productIds);
}
