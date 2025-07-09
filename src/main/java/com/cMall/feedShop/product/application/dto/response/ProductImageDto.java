package com.cMall.feedShop.product.application.dto.response;

import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.model.ProductImage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductImageDto {
    private Long imageId;
    private String url;
    private ImageType type;

    public static ProductImageDto from(ProductImage image) {
        return ProductImageDto.builder()
                .imageId(image.getImageId())
                .url(image.getUrl())
                .type(image.getType())
                .build();
    }

    public static List<ProductImageDto> fromList(List<ProductImage> images)
    {
        return images.stream()
                .map(ProductImageDto::from)
                .toList();
    }
}
