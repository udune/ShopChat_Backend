package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.product.application.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class ProductSellerOptionController {
    private final ProductOptionService productOptionService;

    /**
     * 상품 옵션 삭제 API
     * DELETE /api/seller/products/options/{optionId}
     */
    @DeleteMapping("/products/options/{optionId}")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품 옵션이 성공적으로 삭제되었습니다.")
    public ApiResponse<Void> deleteProductOption(
            @PathVariable Long optionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        productOptionService.deleteProductOption(optionId, userDetails);
        return ApiResponse.success(null);
    }
}
