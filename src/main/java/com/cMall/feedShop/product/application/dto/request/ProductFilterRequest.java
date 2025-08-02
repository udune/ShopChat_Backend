package com.cMall.feedShop.product.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {

    // 카테고리 ID (선택)
    private Long categoryId;

    // 최소 가격 (선택)
    private BigDecimal minPrice;

    // 최대 가격 (선택)
    private BigDecimal maxPrice;

    // 스토어 ID (선택)
    private Long storeId;

    // 가격 범위가 올바른지 검증
    public boolean isValidPriceRange() {
        // 둘 다 null인 경우는 유효한 가격 범위로 간주
        if (minPrice == null && maxPrice == null) {
            return true;
        }

        // 최소 가격이 최대 가격보다 작거나 같으면 유효하다.
        return minPrice.compareTo(maxPrice) <= 0;
    }
}
