package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.application.dto.request.ProductFilterRequest;
import com.cMall.feedShop.product.application.dto.response.ProductPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFilterService {
    public ProductPageResponse filterProductList(ProductFilterRequest request, int page, int size) {

    }
}
