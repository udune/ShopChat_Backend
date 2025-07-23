package com.cMall.feedShop.order.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    @NotBlank(message = "배송 주소는 필수입니다.")
    private String deliveryAddress;

    @NotBlank(message = "배송 상세 주소는 필수입니다.")
    private String deliveryDetailAddress;

    @NotBlank(message = "우편번호는 필수입니다.")
    @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
    private String postalCode;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String recipientName;

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    @Pattern(regexp = "01[0-9]-\\d{4}-\\d{4}", message = "올바른 전화번호 형식이 아닙니다.")
    private String recipientPhone;

    @Min(value = 0, message = "사용할 포인트는 0 이상이어야 합니다.")
    private Integer usedPoints = 0;

    private String deliveryMessage;

    @NotNull(message = "배송비는 필수입니다.")
    @DecimalMin(value = "0", message = "배송비는 0원 이상이어야 합니다.")
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    private BigDecimal totalPrice;

    @NotBlank(message = "결제 방법은 필수입니다.")
    private String paymentMethod;

    private String cardNumber;
    private String cardExpiry;
    private String cardCvc;
}
