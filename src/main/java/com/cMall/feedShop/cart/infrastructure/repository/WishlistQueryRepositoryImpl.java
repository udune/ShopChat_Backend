package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import com.cMall.feedShop.cart.domain.model.QWishList;
import com.cMall.feedShop.cart.domain.model.WishList;
import com.cMall.feedShop.product.domain.model.QProduct;
import com.cMall.feedShop.product.domain.model.QProductImage;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class WishlistQueryRepositoryImpl implements WishlistQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WishlistInfo> findWishlistByUserId(Long userId, Pageable pageable) {
        QWishList wishlist = QWishList.wishList;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;

        // 1. 위시리스트 조회 (Product만 join, 삭제되지 않은 것만)
        List<WishList> wishlists = queryFactory
                .selectFrom(wishlist)
                .join(wishlist.product, product).fetchJoin()
                .where(
                        wishlist.user.id.eq(userId)
                                .and(wishlist.deletedAt.isNull())
                )
                .orderBy(wishlist.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (wishlists.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2. 상품 ID들 추출
        List<Long> productIds = wishlists.stream()
                .map(w -> w.getProduct().getProductId())
                .collect(Collectors.toList());

        // 3. 각 상품의 첫 번째 메인 이미지 조회
        Map<Long, String> imageMap = queryFactory
                .select(productImage.product.productId, productImage.url.min())
                .from(productImage)
                .where(
                        productImage.product.productId.in(productIds)
                                .and(productImage.type.eq(ImageType.MAIN))
                )
                .groupBy(productImage.product.productId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(productImage.product.productId),
                        tuple -> tuple.get(productImage.url.min())
                ));

        // 4. WishlistInfo 변환
        List<WishlistInfo> content = wishlists.stream()
                .map(w -> new WishlistInfo(
                        w.getWishlistId(),
                        w.getProduct().getProductId(),
                        w.getProduct().getName(),
                        imageMap.get(w.getProduct().getProductId()),
                        w.getProduct().getPrice(),
                        w.getProduct().getDiscountType(),
                        w.getProduct().getDiscountValue(),
                        w.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // 5. 전체 개수 조회 (삭제되지 않은 것만)
        Long total = queryFactory
                .select(wishlist.count())
                .from(wishlist)
                .where(
                        wishlist.user.id.eq(userId)
                                .and(wishlist.deletedAt.isNull())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public long countWishlistByUserId(Long userId) {
        QWishList wishlist = QWishList.wishList;

        Long count = queryFactory
                .select(wishlist.count())
                .from(wishlist)
                .where(
                        wishlist.user.id.eq(userId)
                                .and(wishlist.deletedAt.isNull())
                )
                .fetchOne();

        return count != null ? count : 0;
    }

    @Override
    public boolean existsActiveWishlistByUserIdAndProductId(Long userId, Long productId) {
        QWishList wishlist = QWishList.wishList;

        Integer result = queryFactory
                .selectOne()
                .from(wishlist)
                .where(
                        wishlist.user.id.eq(userId)
                                .and(wishlist.product.productId.eq(productId))
                                .and(wishlist.deletedAt.isNull())
                )
                .fetchFirst();

        return result != null;
    }
}