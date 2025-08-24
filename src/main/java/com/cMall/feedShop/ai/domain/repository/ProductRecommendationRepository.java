package com.cMall.feedShop.ai.domain.repository;

import com.cMall.feedShop.ai.domain.model.ProductRecommendation;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {
    List<Product> findAllByOrderByCreatedAtDesc(User user);
}
