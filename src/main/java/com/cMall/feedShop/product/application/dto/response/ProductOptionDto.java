package com.cMall.feedShop.product.application.dto.response;

import com.cMall.feedShop.product.domain.enums.Color;
import com.cMall.feedShop.product.domain.enums.Gender;
import com.cMall.feedShop.product.domain.enums.Size;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductOptionDto {
    private Long optionId;
    private Gender gender;
    private Size size;
    private Color color;
    private Integer stock;

    public static ProductOptionDto from(ProductOption option) {
        return ProductOptionDto.builder()
                .optionId(option.getOptionId())
                .gender(option.getGender())
                .size(option.getSize())
                .color(option.getColor())
                .stock(option.getStock())
                .build();
    }

    public static List<ProductOptionDto> fromList(List<ProductOption> options)
    {
        return options.stream()
                .map(ProductOptionDto::from)
                .toList();
    }
}
