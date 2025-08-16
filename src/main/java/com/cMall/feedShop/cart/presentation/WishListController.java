package com.cMall.feedShop.cart.presentation;

import com.cMall.feedShop.cart.application.service.WishlistService;
import com.cMall.feedShop.common.aop.ApiResponseFormat;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.cMall.feedShop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class WishListController {
    private final WishlistService wishlistService;

    @DeleteMapping("/wishlist/{productId}")
    @ApiResponseFormat(message = "찜한 상품이 성공적으로 취소되었습니다.")
    public ApiResponse<Void> deleteWishList(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = (User) userDetails;
        wishlistService.deleteWishList(productId, currentUser.getUsername());
        return ApiResponse.success(null);
    }
}
