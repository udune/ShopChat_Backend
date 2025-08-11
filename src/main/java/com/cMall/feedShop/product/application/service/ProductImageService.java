package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.common.service.GcpStorageService;
import com.cMall.feedShop.common.validator.ImageValidator;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final GcpStorageService gcpStorageService;
    private final ImageValidator imageValidator;

    /**
     * 상품 이미지 업로드
     * @param product 상품 엔티티
     * @param files 업로드할 이미지 파일 리스트
     * @param type 이미지 타입 (MAIN, DETAIL)
     * @return 업로드된 이미지 리스트
     */
    public List<ProductImage> uploadImages(Product product, List<MultipartFile> files, ImageType type) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        // 이미지 파일 검증
        imageValidator.validateAll(files, getCurrentImageCount(product, type));

        try {
            // GCP Storage에 업로드
            List<GcpStorageService.UploadResult> uploadResults =
                    gcpStorageService.uploadFilesWithDetails(files, "products");

            // ProductImage 엔티티 생성
            return uploadResults.stream()
                    .map(result -> new ProductImage(
                            gcpStorageService.extractObjectName(result.getFilePath()),
                            type,
                            product
                    ))
                    .toList();

        } catch (Exception e) {
            throw new ProductException(ErrorCode.FILE_UPLOAD_ERROR, "이미지 업로드에 실패했습니다.");
        }
    }

    /**
     * 상품 이미지 교체
     * @param product 상품 엔티티
     * @param files 새로 업로드할 이미지 파일 리스트
     * @param type 이미지 타입 (MAIN, DETAIL)
     */
    public void replaceImages(Product product, List<MultipartFile> files, ImageType type) {
        // 해당 타입 이미지들 가져오기
        List<ProductImage> existingImages = getProductImages(product, type);

        // 새 이미지 먼저 업로드
        List<ProductImage> newImages = uploadImages(product, files, type);

        // DB 업데이트
        product.getProductImages().removeAll(existingImages);
        product.getProductImages().addAll(newImages);

        // 기존 파일 삭제
        existingImages.forEach(this::deleteImageFile);
    }

    // 상품 이미지 조회(해당 타입)
    private List<ProductImage> getProductImages(Product product, ImageType type) {
        return product.getProductImages().stream()
                .filter(image -> image.getType() == type)
                .toList();
    }

    // 상품 이미지 개수 조회(해당 타입)
    private int getCurrentImageCount(Product product, ImageType type) {
        return (int) product.getProductImages().stream()
                .filter(image -> image.getType() == type)
                .count();
    }

    // 이미지 파일 삭제
    private void deleteImageFile(ProductImage image) {
        gcpStorageService.deleteFile(gcpStorageService.getFullFilePath(image.getUrl()));
    }
}
