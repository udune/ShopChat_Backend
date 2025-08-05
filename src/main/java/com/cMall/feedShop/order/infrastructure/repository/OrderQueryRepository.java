package com.cMall.feedShop.order.infrastructure.repository;

import com.cMall.feedShop.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQueryRepository {
    // 내 가게 주문 목록 조회
    Page<Order> findOrdersBySellerId(Long sellerId, Pageable pageable);

    // 내 가게 주문 목록 조회 (상태별)
    Page<Order> findOrdersBySellerIdAndStatus(Long sellerId, String status, Pageable pageable);
}
