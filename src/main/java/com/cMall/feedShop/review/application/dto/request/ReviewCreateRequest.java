package com.cMall.feedShop.review.application.dto.request;

import com.cMall.feedShop.review.domain.enums.Cushion;
import com.cMall.feedShop.review.domain.enums.SizeFit;
import com.cMall.feedShop.review.domain.enums.Stability;
import com.cMall.feedShop.review.domain.validation.ValidReviewElements;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ValidReviewElements // 추가된 클래스 레벨 검증
public class ReviewCreateRequest {

    @NotBlank(message = "리뷰 제목은 필수입니다.")
    @Size(max = 100, message = "리뷰 제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
    private Integer rating;

    @NotNull(message = "사이즈 착용감은 필수입니다.")
    private SizeFit sizeFit;

    @NotNull(message = "쿠션감은 필수입니다.")
    private Cushion cushion;

    @NotNull(message = "안정성은 필수입니다.")
    private Stability stability;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "리뷰 내용은 10자 이상 1000자 이하여야 합니다.")
    private String content;

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    // 이미지 업로드 필드 추가
    private List<MultipartFile> images;
}