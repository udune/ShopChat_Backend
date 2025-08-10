package com.cMall.feedShop.product.domain.enums;

public enum ProductSortType {
    LATEST("latest", "최신순"),
    POPULAR("popular", "인기순");

    private final String code;
    private final String description;

    ProductSortType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProductSortType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return LATEST; // 기본값으로 LATEST를 반환
        }
        return "popular".equalsIgnoreCase(code) ? POPULAR : LATEST;
    }
}
