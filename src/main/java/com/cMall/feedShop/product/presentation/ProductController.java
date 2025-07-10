package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.product.application.dto.response.CategoryResponse;
import com.cMall.feedShop.product.application.dto.response.ProductDetailResponse;
import com.cMall.feedShop.product.application.dto.response.ProductPageResponse;
import com.cMall.feedShop.product.application.service.CategoryService;
import com.cMall.feedShop.product.application.service.ProductReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductReadService productReadService;
    private final CategoryService categoryService;

    /**
     * 상품 목록 조회 API
     * /api/products?page=0&size=20
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 상품 목록 응답
     */
    @GetMapping
    public ResponseEntity<ProductPageResponse> getProductList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productReadService.getProductList(page, size));
    }

    /**
     * 상품 상세 조회 API
     * /api/products/{productId}
     * @param productId 상품 아이디
     * @return 상품 상세 조회 응답
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductList(@PathVariable Long productId) {
        return ResponseEntity.ok(productReadService.getProductDetail(productId));
    }

    /**
     * 카테고리 목록 조회 API
     * /api/products/categories
     * @return 카테고리 목록 응답
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
