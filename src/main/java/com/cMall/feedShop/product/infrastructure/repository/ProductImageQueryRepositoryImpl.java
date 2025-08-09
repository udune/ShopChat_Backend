package com.cMall.feedShop.product.infrastructure.repository;

import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.QProductImage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ProductImage QueryDSL 구현체
 * 복잡한 조회 로직들을 QueryDSL로 처리하는 클래스
 */
@Repository
@RequiredArgsConstructor
public class ProductImageQueryRepositoryImpl implements ProductImageQueryRepository {

    // QueryDSL을 사용하기 위한 Factory (쿼리 생성 도구)
    private final JPAQueryFactory queryFactory;

    /**
     * 여러 상품의 메인 이미지들을 한 번에 조회하는 메소드
     *
     * 예시: 상품 A, B, C의 메인 이미지를 각각 따로 조회하지 않고
     *      한 번의 쿼리로 모든 메인 이미지를 가져옴 (성능 향상)
     *
     * @param productIds 조회하려는 상품들의 ID 목록 (중복 제거된 Set)
     * @return 해당 상품들의 메인 이미지 목록 (MAIN 타입만)
     */
    @Override
    public List<ProductImage> findMainImagesByProductIds(Set<Long> productIds) {
        // QueryDSL Q클래스 생성 (테이블과 연결되는 객체)
        QProductImage productImage = QProductImage.productImage;

        // 실제 데이터베이스 쿼리 실행
        return queryFactory
                .selectFrom(productImage)  // ProductImage 테이블에서 모든 컬럼 조회
                .where(
                        // 조건 1: 상품 ID가 우리가 찾는 목록에 포함되어야 함
                        productImage.product.productId.in(productIds)
                                // 조건 2: 이미지 타입이 MAIN이어야 함 (상세 이미지 제외)
                                .and(productImage.type.eq(ImageType.MAIN))
                )
                .fetch();  // 결과를 리스트로 가져오기
    }
}