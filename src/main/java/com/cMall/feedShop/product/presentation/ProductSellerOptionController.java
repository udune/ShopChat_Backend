package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.product.application.dto.request.ProductOptionUpdateRequest;
import com.cMall.feedShop.product.application.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class ProductSellerOptionController {

    private final ProductService productService;

    /**
     * 상품 옵션을 수정하는 API
     * PUT /api/seller/products/options/{optionId}
     *
     * @param optionId 상품 옵션 ID
     * @param request  상품 옵션 수정 요청
     * @param userDetails 인증된 사용자 정보
     * @return 수정된 상품 옵션 정보
     */
    @PutMapping("/products/options/{optionId}")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품 옵션이 성공적으로 수정되었습니다.")
    public ApiResponse<Void> updateProductOption(
            @PathVariable Long optionId,
            @Valid @RequestBody ProductOptionUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        productService.
    }

}
