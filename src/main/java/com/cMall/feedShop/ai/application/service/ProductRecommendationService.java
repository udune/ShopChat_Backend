package com.cMall.feedShop.ai.application.service;

import com.cMall.feedShop.ai.domain.model.ProductRecommendation;
import com.cMall.feedShop.ai.domain.repository.ProductRecommendationRepository;
import com.cMall.feedShop.common.ai.CommonAIService;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductRecommendationService {
    private final CommonAIService aiService;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ObjectMapper objectMapper;

    // AI를 사용하여 사용자 맞춤 상품 추천
    public List<Product> recommendProducts(User user, String prompt, int limit) {
        final int safeLimit = Math.max(1, Math.min(20, limit));
        log.info("AI 추천 요청 - 사용자: {}, 프롬프트: '{}'", user != null ? user.getEmail() : "anonymous", prompt);

        try {
            // AI 프롬프트 생성
            String aiPrompt = buildPrompt(user, prompt, safeLimit);

            // 생성된 AI 프롬프트로 물어보기
            String aiResponse = aiService.generateText(aiPrompt);

            // AI 응답을 Map 으로 파싱
            Map<String, Object> responseMap = aiService.getResponseMap(aiResponse);

            // Map 으로 파싱한 결과에서 상품 ID 추출
            List<Long> recommendedProductIds = extractProductIds(responseMap, safeLimit);

            // 상품 ID로 상품 조회
            List<Product> products = getProductsByIds(recommendedProductIds, safeLimit);

            // 추천 기록 저장
            saveRecommendation(user, prompt, recommendedProductIds, aiResponse);

            return products;
        } catch (Exception e) {
            log.warn("AI 추천 중 오류 발생, 폴백 사용: {}", e.getMessage());
            return getFallbackProducts(safeLimit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractProductIds(Map<String, Object> responseMap, int limit) {
        try {
            Object statusObj = responseMap.get("status");
            if (statusObj != null && !"OK".equalsIgnoreCase(String.valueOf(statusObj))) {
                log.warn("AI 응답 status가 OK가 아님: {}, fallback 사용", statusObj);
                return getFallbackProductIds(limit);
            }

            Object raw = responseMap.get("productIds");
            if (!(raw instanceof List<?> list) || list.isEmpty()) {
                log.warn("AI 응답에서 productIds를 찾을 수 없음, fallback 사용");
                return getFallbackProductIds(limit);
            }

            return ((List<?>) raw).stream()
                    .filter(elem -> elem instanceof Number)
                    .map(elem -> ((Number) elem).longValue())
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패: {}", e.getMessage());
            return getFallbackProductIds(limit);
        }
    }

    private List<Product> getProductsByIds(List<Long> productIds, int limit) {
        if (productIds == null || productIds.isEmpty()) {
            return getFallbackProducts(limit);
        }

        List<Product> products = productRecommendationRepository.findProductsByIds(productIds);

        // id -> product 맵
        Map<Long, Product> byId = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        // AI가 준 순서를 보존하며 수집
        List<Product> ordered = productIds.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .collect(Collectors.toList());

        // 부족하면 폴백으로 채우되 중복 제거
        if (ordered.size() < limit) {
            int need = limit - ordered.size();
            var seen = new java.util.LinkedHashSet<Long>(
                    ordered.stream().map(Product::getProductId).toList()
            );
            List<Product> fallback = getFallbackProducts(Math.max(need * 2, 1));
            for (Product p : fallback) {
                if (ordered.size() >= limit) break;
                Long id = p.getProductId();
                if (seen.add(id)) {        // 처음 본 id만 추가
                    ordered.add(p);
                }
            }
        }

        // 최종적으로 정확히 limit 개로 자르기
        return ordered.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Long> getFallbackProductIds(int limit) {
        return getFallbackProducts(limit).stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());
    }

    private List<Product> getFallbackProducts(int limit) {
        return productRecommendationRepository
                .findAllProductsOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, limit)))
                .getContent();
    }

    private String buildPrompt(User user, String promptInput, int limit) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("""
            신발 쇼핑몰에서 다음 조건을 고려하여 상품 %d개를 추천해주세요:
            
            === 사용자 요청 ===
            "%s"
            """, limit, promptInput));

        // 사용자 프로필 정보가 있으면 추가
        UserProfile profile = user != null ? user.getUserProfile() : null;
        if (profile != null) {
            prompt.append("\n=== 사용자 정보 ===\n");

            if (profile.getFootSize() != null) {
                prompt.append(String.format("- 발 크기: %dmm\n", profile.getFootSize()));
            }

            if (profile.getFootWidth() != null) {
                prompt.append(String.format("- 발 너비: %s\n", profile.getFootWidth()));
            }

            if (profile.getFootArchType() != null) {
                prompt.append(String.format("- 발등 높이: %s\n", profile.getFootArchType()));
            }

            if (profile.getGender() != null) {
                prompt.append(String.format("- 성별: %s\n", profile.getGender()));
            }

            if (profile.getHeight() != null) {
                prompt.append(String.format("- 키: %dcm\n", profile.getHeight()));
            }

            if (profile.getWeight() != null) {
                prompt.append(String.format("- 체중: %dkg\n", profile.getWeight()));
            }
        }

        prompt.append("""
            
            === 추천 기준 ===
            - 사용자의 발 사이즈에 맞는 상품 우선 추천
            - 발 너비와 발등 높이를 고려한 편안한 핏
            - 성별과 체형에 적합한 디자인
            - 현재 재고가 있는 상품만 추천
            
            응답은 다음 JSON 형식으로만 작성해주세요:
            {"status":"OK", "productIds": [1, 2, 3, 4, 5], "message": ""}
            """);

        return prompt.toString();
    }

    private void saveRecommendation(User user, String prompt, List<Long> productIds, String response) {
        try {
            String productIdsJson = objectMapper.writeValueAsString(productIds);
            ProductRecommendation recommendation = ProductRecommendation.builder()
                    .user(user)
                    .prompt(prompt)
                    .recommendedProductIds(productIdsJson)
                    .response(response)
                    .build();
            productRecommendationRepository.save(recommendation);
        } catch (Exception e) {
            log.error("추천 이력 저장 실패", e);
        }
    }
}
