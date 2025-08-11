package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.common.service.GcpStorageService;
import com.cMall.feedShop.product.application.dto.response.ProductImageUploadResponse;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final GcpStorageService gcpStorageService;
    private final UserRepository userRepository;

    @Transactional
    public ProductImageUploadResponse uploadProductImage(MultipartFile imageFile, ImageType type, UserDetails userDetails) {
        // 1. 사용자 정보를 가져온다.
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한 검증을 한다.
        validateSellerRole(currentUser);

        // 3. 이미지 파일 검증을 한다.
        validateImageFile(imageFile);

        // 4. 이미지 파일을 GCP에 업로드한다.
        String imageUrl = uploadImageToGCP(imageFile, "products");

        // 5. 업로드된 이미지 URL과 기타 정보를 포함한 응답 객체를 생성한다.
        return ProductImageUploadResponse.of(
            imageUrl, type, imageFile.getOriginalFilename(), imageFile.getSize()
        );
    }

    public void deleteProductImage(String imageUrl, UserDetails userDetails) {
        // 1. 사용자 정보를 가져온다.
        User currentUser = getCurrentUser(userDetails);

        // 2. 판매자 권한 검증을 한다.
        validateSellerRole(currentUser);

        // 3. GCP 에서 이미지 파일을 삭제한다.
        try {
            gcpStorageService.deleteFile(imageUrl);
        } catch (Exception e) {
            throw new ProductException(ErrorCode.FILE_DELETE_ERROR, "이미지 삭제에 실패했습니다." + e.getMessage());
        }
    }

    // 사용자 정보를 가져온다.
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND));
    }

    // 판매자 권한을 검증한다.
    private void validateSellerRole(User user) {
        if (user.getRole() != UserRole.SELLER) {
            throw new ProductException(ErrorCode.FORBIDDEN);
        }
    }

    // 이미지 파일 검증을 한다.
    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일이 비어있습니다.");
        }
    }

    // 이미지 파일을 GCP 에 업로드한다.
    private String uploadImageToGCP(MultipartFile imageFile, String path) {
        try {
            GcpStorageService.UploadResult uploadResult = gcpStorageService.uploadFile(imageFile, path);
            if (uploadResult == null || uploadResult.getFilePath() == null || uploadResult.getFilePath().isEmpty() ) {
                throw new ProductException(ErrorCode.FILE_UPLOAD_ERROR, "이미지 업로드에 실패했습니다.");
            }
            return uploadResult.getFilePath();
        } catch (Exception e) {
            throw new ProductException(ErrorCode.FILE_UPLOAD_ERROR, "이미지 업로드에 실패했습니다." + e.getMessage());
        }
    }
}
