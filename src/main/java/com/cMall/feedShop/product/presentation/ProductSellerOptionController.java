package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.product.application.dto.request.ProductOptionUpdateRequest;
import com.cMall.feedShop.product.application.dto.response.info.ProductOptionInfo;
import com.cMall.feedShop.product.application.service.ProductOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class ProductSellerOptionController {

    private final ProductOptionService productOptionService;

    /**
     * 상품 옵션 정보를 조회하는 API
     * GET /api/seller/products/{productId}/options
     *
     * @param productId 조회할 상품의 ID
     * @return 상품 옵션 정보 리스트
     */
    @GetMapping("/products/{productId}/options")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "상품 옵션 정보를 조회했습니다.")
    public ApiResponse<List<ProductOptionInfo>> getProductOptions(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 상품 ID로 상품 옵션 정보를 조회
        List<ProductOptionInfo> options = productOptionService.getProductOptions(productId, userDetails);

        // 조회된 옵션 정보를 ApiResponse로 감싸서 반환
        return ApiResponse.success(options);
    }

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
        productOptionService.updateProductOption(optionId, request, userDetails);
        return ApiResponse.success(null);
    }
}
