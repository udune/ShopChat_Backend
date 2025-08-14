package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.application.dto.response.ProductPageResponse;
import com.cMall.feedShop.product.application.service.ProductReadService;
import com.cMall.feedShop.product.application.service.ProductService;
import com.cMall.feedShop.user.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
//@Tag(name = "상품 관리(판매자)", description = "판매자가 상품을 등록하고 수정하는 API 입니다.")
public class ProductSellerController {
    private final ProductService productService;
    private final ProductReadService productReadService;

    /**
     * 상품 목록 조회 API
     * GET /api/seller/products
     */
    @GetMapping("/products")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품 목록이 성공적으로 조회되었습니다.")
    public ApiResponse<ProductPageResponse> getProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = (User) userDetails;
        ProductPageResponse data = productReadService.getSellerProductList(page, size, currentUser.getUsername());
        return ApiResponse.success(data);
    }

    /**
     * 상품 등록 API (이미지와 옵션 포함)
     * POST /api/seller/products
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품이 성공적으로 등록되었습니다.")
    public ApiResponse<ProductCreateResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProductCreateResponse data = productService.createProduct(request, userDetails);
        return ApiResponse.success(data);
    }

    /**
     * 상품 수정 API
     * PUT /api/seller/products/{productId}
     */
    @PutMapping("/products/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품이 성공적으로 수정되었습니다.")
    public ApiResponse<Void> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        productService.updateProduct(productId, request, userDetails);
        return ApiResponse.success(null);
    }

    /**
     * 상품 삭제 API
     * DELETE /api/seller/products/{productId}
     */
    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품이 성공적으로 삭제되었습니다.")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails)
    {
        productService.deleteProduct(productId, userDetails);
        return ApiResponse.success(null);
    }
}
