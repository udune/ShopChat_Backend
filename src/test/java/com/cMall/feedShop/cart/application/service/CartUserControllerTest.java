package com.cMall.feedShop.cart.application.service;

import com.cMall.feedShop.cart.application.dto.request.CartItemCreateRequest;
import com.cMall.feedShop.cart.application.dto.response.CartItemResponse;
import com.cMall.feedShop.cart.application.service.CartService;
import com.cMall.feedShop.cart.presentation.CartUserController;
import com.cMall.feedShop.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartUserController.class)
@DisplayName("CartUserController 테스트")
class CartUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CartItemCreateRequest request;
    private CartItemResponse response;

    @BeforeEach
    void setUp() {
        request = new CartItemCreateRequest();
        ReflectionTestUtils.setField(request, "optionId", 1L);
        ReflectionTestUtils.setField(request, "imageId", 1L);
        ReflectionTestUtils.setField(request, "quantity", 2);

        response = CartItemResponse.builder()
                .cartItemId(1L)
                .cartId(1L)
                .optionId(1L)
                .imageId(1L)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("장바구니 아이템 추가 성공")
    void addCartItem_Success() throws Exception {
        // given
        given(cartService.addCartItem(any(CartItemCreateRequest.class), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/users/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품이 장바구니에 추가되었습니다."))
                .andExpect(jsonPath("$.data.cartItemId").value(1L))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("장바구니 아이템 추가 실패 - 유효성 검사 오류 (옵션 ID 없음)")
    void addCartItem_Fail_ValidationError_NoOptionId() throws Exception {
        // given
        CartItemCreateRequest invalidRequest = new CartItemCreateRequest();
        ReflectionTestUtils.setField(invalidRequest, "imageId", 1L);
        ReflectionTestUtils.setField(invalidRequest, "quantity", 2);

        // when & then
        mockMvc.perform(post("/api/users/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("장바구니 아이템 추가 실패 - 유효성 검사 오류 (수량 0)")
    void addCartItem_Fail_ValidationError_ZeroQuantity() throws Exception {
        // given
        CartItemCreateRequest invalidRequest = new CartItemCreateRequest();
        ReflectionTestUtils.setField(invalidRequest, "optionId", 1L);
        ReflectionTestUtils.setField(invalidRequest, "imageId", 1L);
        ReflectionTestUtils.setField(invalidRequest, "quantity", 0);

        // when & then
        mockMvc.perform(post("/api/users/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("장바구니 아이템 추가 실패 - 인증되지 않은 사용자")
    void addCartItem_Fail_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(post("/api/users/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}