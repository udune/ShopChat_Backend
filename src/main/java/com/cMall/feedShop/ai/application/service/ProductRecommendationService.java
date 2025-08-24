package com.cMall.feedShop.ai.application.service;

import com.cMall.feedShop.ai.domain.exception.AIException;
import com.cMall.feedShop.ai.domain.model.ProductRecommendation;
import com.cMall.feedShop.ai.domain.repository.ProductRecommendationRepository;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.model.UserProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
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
    private final ChatModel chatModel;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.recommendation.mode:ai}")
    private String mode;

    public List<Product> recommendProducts(User user, String prompt, int limit) {
        log.info("AI 추천 요청 - 사용자: {}, 프롬프트: '{}'", user.getEmail(), prompt);

        try {
            // 1. AI 모델을 사용하여 추천 로직 구현 (예: OpenAI API 호출)
            List<Long> recommendedProductIds = process(user, prompt, limit);

            // 2. 상품 조회
            List<Product> products = getProductsByIds(recommendedProductIds, limit);

            // 3. 추천 기록 저장
            saveRecommendation(user, prompt, recommendedProductIds, "AI 추천 응답 저장됨");

            return products;
        } catch (Exception e) {
            log.error("AI 추천 중 오류 발생: {}", e.getMessage());
            throw new AIException(ErrorCode.PRODUCT_RECOMMENDATION_FAILED);
        }
    }

    private List<Long> process(User user, String prompt, int limit) {
        if ("mock".equals(mode)) {
            return getMockRecommendation(prompt, limit);
        } else {
            return getRecommendation(user, prompt, limit);
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

        List<Product> products = productRecommendationRepository.findProductsByIds(productIds);

        if (products.size() < limit) {
            List<Product> additional = getFallbackProducts(limit - products.size());
            products.addAll(additional);
        }

        return products.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Product> getFallbackProducts(int limit) {
        return productRecommendationRepository.findAllProductsOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
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

    private List<Long> getRecommendation(User user, String prompt, int limit) {
        String promptInput = buildPrompt(user, prompt, limit);

        try {
            ChatResponse response = chatModel.call(new Prompt(promptInput));
            String content = response.getResult().getOutput().getContent();
            return extractProductIds(content, limit);
        } catch (Exception e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            return getMockRecommendation(prompt, limit);
        }
    }

    private String buildPrompt(User user, String promptInput, int limit) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("""
            신발 쇼핑몰에서 다음 조건을 고려하여 상품 %d개를 추천해주세요:
            
            === 사용자 요청 ===
            "%s"
            """, limit, promptInput));

        // 사용자 프로필 정보가 있으면 추가
        UserProfile profile = user.getUserProfile();
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
            {"productIds": [1, 2, 3, 4, 5]}
            """);

        return prompt.toString();
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
