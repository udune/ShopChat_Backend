package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.domain.model.QWishList;
import com.cMall.feedShop.cart.domain.model.WishList;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WishlistQueryRepositoryImpl implements WishlistQueryRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<WishList> findByUserIdAndProductId(Long userId, Long productId) {
        QWishList wishList = QWishList.wishList;

        WishList result = queryFactory
                .selectFrom(wishList)
                .where(
                        wishList.user.id.eq(userId)
                                .and(wishList.product.productId.eq(productId))
                                .and(wishList.deletedAt.isNull())
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
