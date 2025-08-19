package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.domain.model.QWishList;
import com.cMall.feedShop.cart.domain.model.WishList;
import com.cMall.feedShop.product.domain.model.QProduct;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WishlistQueryRepositoryImpl implements WishlistQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public void increaseWishCount(Long productId) {
        QProduct product = QProduct.product;

        queryFactory
                .update(product)
                .set(product.wishNumber, product.wishNumber.add(1))
                .where(product.productId.eq(productId))
                .execute();
    }

    @Override
    public void decreaseWishCount(Long productId) {
        QProduct product = QProduct.product;

        queryFactory
                .update(product)
                .set(product.wishNumber,
                        new CaseBuilder()
                                .when(product.wishNumber.gt(0))
                                .then(product.wishNumber.subtract(1))
                                .otherwise(0))
                .where(product.productId.eq(productId))
                .execute();
    }

    @Override
    public Optional<WishList> findByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId) {
        QWishList wishList = QWishList.wishList;

        WishList result = queryFactory
                .selectFrom(wishList)
                .where(
                        wishList.user.id.eq(userId)
                                .and(wishList.product.productId.eq(productId))  // productId 명시적 사용
                                .and(wishList.deletedAt.isNull())
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

}