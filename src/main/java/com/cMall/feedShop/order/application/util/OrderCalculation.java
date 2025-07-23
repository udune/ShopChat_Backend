package com.cMall.feedShop.order.application.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class OrderCalculation {
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private Integer actualUsedPoints;
    private Integer earnedPoints;
}
