package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.application.dto.request.ProductUpdateRequest;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.model.Category;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.repository.ProductRepository;
import com.cMall.feedShop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductUpdateService {

    private final ProductRepository productRepository;
    private final ProductHelper productHelper;
    private final ProductImageService productImageService;

    public void updateProduct(Long productId, ProductUpdateRequest request, List<MultipartFile> mainImages, List<MultipartFile> detailImages, String loginId) {
        // 1. 현재 사용자 정보 가져오기 및 권한 검증
        User currentUser = productHelper.getCurrentUser(loginId);

        // 2. 판매자 권한 검증
        productHelper.validateSellerRole(currentUser);

        // 3. 상품 조회 (소유권 검증 포함)
        Product product = productHelper.getProductOwnership(productId, currentUser.getId());

        // 4. 카테고리 존재 확인
        Category category = null;
        if (request.getCategoryId() != null) {
            category = productHelper.getCategory(request.getCategoryId());
        }

        // 5. 상품명 중복 확인
        productHelper.validateNameChange(product, request.getName(), productId);

        // 6. 상품 필드 업데이트
        productHelper.updateBasicInfo(product, request, category);

        // 7. 이미지 업데이트
        if (mainImages != null) {
            productImageService.replaceImages(product, mainImages, ImageType.MAIN);
        }

        if (detailImages != null) {
            productImageService.replaceImages(product, detailImages, ImageType.DETAIL);
        }

        // 8. 옵션 업데이트
        if (request.getOptions() != null) {
            productHelper.replaceOptions(product, request.getOptions());
        }

        // 9. DB 저장
        productRepository.save(product);
    }
}