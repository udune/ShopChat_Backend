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

    // 최대 파일 크기 (10MB)
    private long maxFileSize = 10 * 1024 * 1024L;

    // 최대 이미지 개수 (10개)
    private int maxImageCount = 10;

    // 허용되는 이미지 확장자
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");

    // 허용되는 Content-Type
    private List<String> allowedContentTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

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
        validateImages(files);

        // 이미지 개수 검증
        validateImageCount(getCurrentImageCount(product, type), files.size());

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

    // 이미지 파일 검증
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일이 필요합니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("이미지 크기는 %dMB를 초과할 수 없습니다.",
                            maxFileSize / (1024 * 1024)));
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "파일명이 없습니다.");
        }

        // 확장자 추출 및 검증
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 이미지 형식입니다. 지원 형식: " + allowedExtensions);
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "올바른 이미지 파일이 아닙니다.");
        }
    }

    // 이미지 파일 리스트 검증
    public void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            validateImage(file);
        }
    }

    // 이미지 개수 검증
    public void validateImageCount(int currentCount, int newCount) {
        int totalCount = currentCount + newCount;
        if (totalCount > maxImageCount) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("이미지는 최대 %d개까지만 업로드할 수 있습니다.", maxImageCount));
        }
    }

    // 파일 확장자 추출
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "파일 확장자가 없습니다.");
        }
        if (lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
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
        deleteImageFiles(existingImages);
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
