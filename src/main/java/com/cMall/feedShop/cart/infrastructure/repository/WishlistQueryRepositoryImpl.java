package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.application.dto.response.info.WishlistInfo;
import com.cMall.feedShop.cart.domain.model.QWishList;
import com.cMall.feedShop.product.domain.enums.ImageType;
import com.querydsl.core.types.Projections;
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

    @Override
    public Page<WishlistInfo> findWishlistByUserId(Long userId, Pageable pageable) {
        List<WishlistInfo> content = queryFactory
                .select(Projections.constructor(
                        WishlistInfo.class,
                        wishList.wishlistId,
                        product.productId,
                        product.name,
                        productImage.url.coalesce(""),
                        product.price,
                        product.discountType,
                        product.discountValue,
                        wishList.createdAt
                ))
                .from(wishList)
                .join(product).on(wishList.product.productId.eq(product.productId))
                .leftJoin(productImage).on(
                        product.productId.eq(productImage.product.productId)
                                .and(productImage.type.eq(ImageType.MAIN))
                )
                .where(wishList.user.id.eq(userId)
                        .and(wishList.deletedAt.isNull()))
                .orderBy(wishList.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(wishList.count())
                .from(wishList)
                .join(product).on(wishList.product.productId.eq(product.productId))
                .where(wishList.user.id.eq(userId)
                        .and(wishList.deletedAt.isNull()))
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
