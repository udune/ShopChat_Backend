package com.cMall.feedShop.order.infrastructure.repository;

import com.cMall.feedShop.order.domain.enums.OrderStatus;
import com.cMall.feedShop.order.domain.model.Order;
import com.cMall.feedShop.order.domain.model.QOrder;
import com.cMall.feedShop.order.domain.model.QOrderItem;
import com.cMall.feedShop.order.infrastructure.repository.OrderQueryRepository;
import com.cMall.feedShop.product.domain.model.QProduct;
import com.cMall.feedShop.product.domain.model.QProductImage;
import com.cMall.feedShop.product.domain.model.QProductOption;
import com.cMall.feedShop.store.domain.model.QStore;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findOrdersBySellerId(Long sellerId, Pageable pageable) {
        return findOrdersBySellerIdWithCondition(sellerId, null, pageable);
    }

    @Override
    public Page<Order> findOrdersBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable) {
        return findOrdersBySellerIdWithCondition(sellerId, status, pageable);
    }

    private Page<Order> findOrdersBySellerIdWithCondition(Long sellerId, OrderStatus status, Pageable pageable) {
        QOrder order = QOrder.order;

        List<Order> orders = createBaseQuery()
                .where(
                        sellerIdEq(sellerId),
                        statusEq(status)
                )
                .orderBy(order.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = createCountQuery()
                .where(
                        sellerIdEq(sellerId),
                        statusEq(status)
                );

        return PageableExecutionUtils.getPage(orders, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Order> createBaseQuery() {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProductOption productOption = QProductOption.productOption;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;
        QStore store = QStore.store;

        return queryFactory
                .selectFrom(order)
                .distinct()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.productOption, productOption).fetchJoin()
                .leftJoin(productOption.product, product).fetchJoin()
                .leftJoin(product.store, store).fetchJoin()
                .leftJoin(orderItem.productImage, productImage).fetchJoin();
    }

    private JPAQuery<Long> createCountQuery() {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProductOption productOption = QProductOption.productOption;
        QProduct product = QProduct.product;
        QStore store = QStore.store;

        return queryFactory
                .select(order.countDistinct())
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .leftJoin(orderItem.productOption, productOption)
                .leftJoin(productOption.product, product)
                .leftJoin(product.store, store);
    }

    private BooleanExpression sellerIdEq(Long sellerId) {
        QStore store = QStore.store;
        return sellerId != null ? store.sellerId.eq(sellerId) : null;
    }

    private BooleanExpression statusEq(OrderStatus status) {
        QOrder order = QOrder.order;
        return status != null ? order.status.eq(status) : null;
    }
}