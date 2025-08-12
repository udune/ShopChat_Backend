package com.cMall.feedShop.product.infrastructure.repository;

import com.cMall.feedShop.product.domain.enums.ProductSortType;
import com.cMall.feedShop.product.domain.model.Product;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.cMall.feedShop.product.domain.model.QCategory.category;
import static com.cMall.feedShop.product.domain.model.QProduct.product;
import static com.cMall.feedShop.store.domain.model.QStore.store;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public long countByKeyword(String keyword) {
        BooleanBuilder whereClause = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            whereClause.and(product.name.containsIgnoreCase(keyword.trim()));
        }

        Long count = jpaQueryFactory
                .select(product.count())
                .from(product)
                .where(whereClause)
                .fetchOne();

        return count != null ? count : 0;
    }

    @Override
    public long countWithFilters(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Long storeId) {
        BooleanBuilder whereClause = createWhereClause(categoryId, minPrice, maxPrice, storeId);

        Long count = jpaQueryFactory
                .select(product.count())
                .from(product)
                .where(whereClause)
                .fetchOne();

        return count != null ? count : 0;
    }

    @Override
    public long countAll() {
        Long count = jpaQueryFactory
                .select(product.count())
                .from(product)
                .fetchOne();

        return count != null ? count : 0;
    }

    @Override
    public Page<Product> searchProductsByName(String keyword, Pageable pageable) {
        BooleanBuilder whereClause = new BooleanBuilder();

        // 키워드가 있으면 상품명 검색 조건을 추가한다. (부분 일치, 대소문자 구분 없음)
        // 키워드가 없으면 검색 조건을 추가하지 않는다. => 모든 상품을 조회한다.
        if (StringUtils.hasText(keyword)) {
            whereClause.and(product.name.containsIgnoreCase(keyword.trim()));
        }

        List<Product> products = jpaQueryFactory
                .selectFrom(product)
                .leftJoin(product.store, store).fetchJoin()  // 스토어 정보 즉시 로딩
                .leftJoin(product.category, category).fetchJoin()  // 카테고리 정보 즉시 로딩
                .leftJoin(product.productImages).fetchJoin()  // 이미지 정보 즉시 로딩
                .where(whereClause)  // 동적 조건 적용
                .orderBy(product.createdAt.desc())  // 최신순 정렬
                .offset(pageable.getOffset())  // 페이징 시작점
                .limit(pageable.getPageSize())  // 페이지 크기
                .fetch();

        // 전체 개수 조회
        Long totalCount = jpaQueryFactory
                .select(product.count())
                .from(product)
                .where(whereClause)  // 같은 조건 적용
                .fetchOne();

        return new PageImpl<>(products, pageable, totalCount != null ? totalCount : 0);
    }

    @Override
    public Page<Product> findProductsWithFilters(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long storeId,
            ProductSortType productSortType,
            Pageable pageable
    ) {
        // 1. 동적 조건 생성
        BooleanBuilder whereClause = createWhereClause(categoryId, minPrice, maxPrice, storeId);
        OrderSpecifier<?> orderBy = getOrderSpecifier(productSortType);

        // 2. 상품 목록 조회
        List<Product> products = jpaQueryFactory
                .selectFrom(product)
                .leftJoin(product.store, store).fetchJoin()  // 스토어 정보 즉시 로딩
                .leftJoin(product.category, category).fetchJoin()  // 카테고리 정보 즉시 로딩
                .leftJoin(product.productImages).fetchJoin()  // 이미지 정보 즉시 로딩
                .where(whereClause)  // 동적 조건 적용
                .orderBy(orderBy)  // 최신순 정렬
                .offset(pageable.getOffset())  // 페이징 시작점
                .limit(pageable.getPageSize())  // 페이지 크기
                .fetch();

        // 3. 전체 개수 조회
        Long totalCount = jpaQueryFactory
                .select(product.count())
                .from(product)
                .where(whereClause)  // 같은 조건 적용
                .fetchOne();

        // 4. Page 객체 생성 및 반환
        return new PageImpl<>(products, pageable, totalCount != null ? totalCount : 0);
    }

    private BooleanBuilder createWhereClause(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long storeId
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 카테고리 필터링 (선택)
        if (categoryId != null) {
            builder.and(product.category.categoryId.eq(categoryId));
        }

        // 2. 최소 가격 필터링 (선택)
        if (minPrice != null) {
            builder.and(product.price.goe(minPrice));
        }

        // 3. 최대 가격 필터링 (선택)
        if (maxPrice != null) {
            builder.and(product.price.loe(maxPrice));
        }

        // 4. 스토어 필터링 (선택)
        if (storeId != null) {
            builder.and(product.store.storeId.eq(storeId));
        }

        return builder;
    }

    private OrderSpecifier<?> getOrderSpecifier(ProductSortType sortType) {
        return sortType == ProductSortType.POPULAR
                ? product.wishNumber.desc()
                : product.createdAt.desc();
    }
}
