package com.cMall.feedShop.product.presentation;

import com.cMall.feedShop.product.application.dto.request.ProductCreateRequest;
import com.cMall.feedShop.product.application.dto.response.ProductCreateResponse;
import com.cMall.feedShop.product.application.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class ProductSellerController {
    private final ProductService productService;

    @PostMapping("/products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductCreateResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }
}
