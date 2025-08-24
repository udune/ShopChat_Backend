package com.cMall.feedShop.ai.application.service;

import com.cMall.feedShop.ai.domain.model.ProductRecommendation;
import com.cMall.feedShop.ai.domain.repository.ProductRecommendationRepository;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.user.domain.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ChatClient chatClient;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.recommendation.mode:ai}")
    private String mode;

    public List<Product> recommendProducts(User user, String prompt, int limit) {
        log.info("AI 추천 요청 - 사용자: {}, 프롬프트: '{}'", user.getEmail(), prompt);

        try {
            // 1. AI 모델을 사용하여 추천 로직 구현 (예: OpenAI API 호출)
            List<Long> recommendedProductIds = process(prompt, limit);

            // 2. 상품 조회
            List<Product> products = getProductsByIds(recommendedProductIds, limit);

            // 3. 추천 기록 저장
            saveRecommendation(user, prompt, recommendedProductIds, "AI 추천 응답 저장됨");

            return products;
        } catch (Exception e) {
            log.error("AI 추천 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("상품 추천 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

    }

    private List<Long> process(String prompt, int limit) {
        if ("mock".equals(mode)) {
            return getMockRecommendation(prompt, limit);
        } else {
            return getRecommendation(prompt, limit);
        }
    }

    private List<Long> getMockRecommendation(String prompt, int limit) {
        if (prompt.toLowerCase().contains("러닝")) {
            return List.of(1L, 2L, 3L, 4L, 5L).stream().limit(limit).collect(Collectors.toList());
        }
        return List.of(1L, 3L, 5L, 7L, 9L).stream().limit(limit).collect(Collectors.toList());
    }

    private List<Product> getProductsByIds(List<Long> productIds, int limit) {
        if (productIds.isEmpty()) {
            return getFallbackProducts(limit);
        }

        List<Product> products = productRecommendationRepository.findAllById(productIds);

        if (products.size() < limit) {
            List<Product> additional = getFallbackProducts(limit - products.size());
            products.addAll(additional);
        }

        return products.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Product> getFallbackProducts(int limit) {
        return productRecommendationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
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

    private List<Long> getRecommendation(String prompt, int limit) {
        String promptInput = buildPrompt(prompt, limit);

        String response = chatClient.prompt()
                .user(promptInput)
                .call()
                .content();

        return extractProductIds(response, limit);
    }

    private String buildPrompt(String promptInput, int limit) {
        return String.format("""
            신발 쇼핑몰에서 다음 요청에 맞는 상품 %d개를 추천해주세요:
            "%s"
            
            응답은 다음 JSON 형식으로만 작성해주세요:
            {"productIds": [1, 2, 3, 4, 5]}
            """, limit, promptInput);
    }

    private List<Long> extractProductIds(String aiResponse, int limit) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    cleanJsonResponse(aiResponse), new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Integer> productIds = (List<Integer>) responseMap.get("productIds");

            return productIds.stream()
                    .map(Integer::longValue)
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패", e);
            return getMockRecommendation("", limit);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response.contains("{")) {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            return response.substring(start, end);
        }
        return response;
    }
}
