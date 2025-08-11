package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.product.application.dto.response.ProductImageUploadResponse;
import com.cMall.feedShop.product.application.service.ProductImageService;
import com.cMall.feedShop.product.domain.enums.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class ProductSellerImageController {

    private final ProductImageService productImageService;

    @PostMapping("/products/images/upload")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponseFormat(message = "이미지 업로드 완료")
    public ApiResponse<ProductImageUploadResponse> uploadProductImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("type") ImageType type,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProductImageUploadResponse data = productImageService.uploadProductImage(imageFile, type, userDetails);
        return ApiResponse.success(data);
    }

    @DeleteMapping("/products/images")
    @PreAuthorize("hasRole('SELLER')")
    @ApiResponseFormat(message = "이미지 삭제 완료")
    public ApiResponse<Void> deleteProductImage(
            @RequestParam("url") String imageUrl,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        productImageService.deleteProductImage(imageUrl, userDetails);
        return ApiResponse.success(null);
    }

}
