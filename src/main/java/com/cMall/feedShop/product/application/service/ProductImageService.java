package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.common.service.GcpStorageService;
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
        // 기존 이미지 제거
        List<ProductImage> existingImages = product.getProductImages().stream()
                .filter(image -> image.getType() == type)
                .toList();

        product.getProductImages().removeAll(existingImages);

        // 새 이미지 추가
        if (files != null && !files.isEmpty()) {
            List<ProductImage> newImages = uploadImages(product, files, type);
            product.getProductImages().addAll(newImages);
        }

        // 기존 파일 삭제
        deleteImageFiles(existingImages);
    }

    // 이미지 파일 삭제
    private void deleteImageFiles(List<ProductImage> images) {
        for (ProductImage image : images) {
            try {
                gcpStorageService.deleteFile(gcpStorageService.getFullFilePath(image.getUrl()));
            } catch (Exception e) {
                throw new ProductException(ErrorCode.FILE_DELETE_ERROR, "이미지 삭제에 실패했습니다: " + image.getUrl());
            }
        }
    }
}
