package com.cMall.feedShop.cart.infrastructure.repository;

import com.cMall.feedShop.cart.domain.model.QWishList;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
