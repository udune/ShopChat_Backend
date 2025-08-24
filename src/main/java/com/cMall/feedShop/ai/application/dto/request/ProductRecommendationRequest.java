package com.cMall.feedShop.ai.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductRecommendationRequest {

    @NotBlank(message = "어떤 신발을 찾고 있는지 알려주세요.")
    private String prompt;

    @Min(value = 1, message = "최소 1개 이상")
    @Max(value = 16, message = "최대 16개 이하")
    private int limit = 8;
}
