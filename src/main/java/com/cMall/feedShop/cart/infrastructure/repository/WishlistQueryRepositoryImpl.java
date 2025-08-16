package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import com.cMall.feedShop.cart.domain.model.QWishList;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.cMall.feedShop.product.domain.model.QProduct;
import com.cMall.feedShop.product.domain.model.QProductImage;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.cMall.feedShop.cart.domain.model.QWishList.wishList;
import static com.cMall.feedShop.product.domain.model.QProduct.product;
import static com.cMall.feedShop.product.domain.model.QProductImage.productImage;

@Repository
@RequiredArgsConstructor
public class WishlistQueryRepositoryImpl implements WishlistQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WishlistInfo> findWishlistByUserId(Long userId, Pageable pageable) {
        QWishList wishList = QWishList.wishList;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;

        JPAQuery<String> mainImageSubQuery = queryFactory
                .select(productImage.url)
                .from(productImage)
                .where(productImage.product.eq(product)
                        .and(productImage.type.eq(ImageType.MAIN)))
                .orderBy(productImage.imageId.asc())
                .limit(1);

        List<WishlistInfo> content = queryFactory
                .select(Projections.constructor(
                        WishlistInfo.class,
                        wishList.wishlistId,
                        product.productId,
                        product.name,
                        mainImageSubQuery,
                        product.price,
                        product.discountType,
                        product.discountValue,
                        wishList.createdAt
                ))
                .from(wishList)
                .join(wishList.product, product)
                .where(wishList.user.id.eq(userId))
                .orderBy(wishList.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(wishList.count())
                .from(wishList)
                .where(wishList.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public long countWishlistByUserId(Long userId) {
        Long count = queryFactory
                .select(wishList.count())
                .from(wishList)
                .join(product).on(wishList.product.productId.eq(product.productId))
                .where(wishList.user.id.eq(userId)
                        .and(wishList.deletedAt.isNull()))
                .fetchOne();

        return count != null ? count : 0;
    }
}
