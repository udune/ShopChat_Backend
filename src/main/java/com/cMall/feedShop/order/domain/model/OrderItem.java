package com.cMall.feedShop.order.domain.model;

import com.cMall.feedShop.common.BaseTimeEntity;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_image_id", nullable = false)
    private ProductImage productImage;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "discount_price", nullable = false)
    private BigDecimal discountPrice;

    @Builder
    public OrderItem(Order order, Long optionId, Long imageId, Integer quantity, BigDecimal price, BigDecimal discountPrice) {
        this.order = order;
        this.optionId = optionId;
        this.imageId = imageId;
        this.quantity = quantity;
        this.price = price;
        this.discountPrice = discountPrice;
    }
}
