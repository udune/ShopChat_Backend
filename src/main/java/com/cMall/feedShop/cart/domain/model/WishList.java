package com.cMall.feedShop.cart.domain.model;

import com.cMall.feedShop.common.BaseTimeEntity;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.user.domain.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist")
@Getter
@NoArgsConstructor
public class WishList extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long wishlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public WishList(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
