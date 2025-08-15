package com.cMall.feedShop.cart.application.dto.response.info;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WishlistInfo {
    private Long wishlistId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private BigDecimal discountValue;
    private LocalDateTime createdAt;
}
