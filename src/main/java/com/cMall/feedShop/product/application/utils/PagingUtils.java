package com.cMall.feedShop.product.application.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PagingUtils {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    /**
     * 유효한 Pageable 객체 생성
     */
    public static Pageable createPageable(int page, int size) {
        int validPage = Math.max(page, DEFAULT_PAGE);
        int validSize = (size < 1 || size > MAX_SIZE) ? DEFAULT_SIZE : size;
        return PageRequest.of(validPage, validSize);
    }
}
