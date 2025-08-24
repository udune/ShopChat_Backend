package com.cMall.feedShop.common.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonAIService {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    private final String fallbackText = "{\"message\": \"AI 서비스가 현재 사용 불가능합니다.\"}";

    // AI 모델을 사용하여 텍스트 생성, 실패 시 폴백 텍스트 반환
    public String generateText(String prompt) {
        if (!aiEnabled || chatModel == null) {
            log.info("AI 비활성화 상태 (aiEnabled: {}, chatModel: {}), 폴백 반환",
                    aiEnabled, chatModel != null ? "있음" : "없음");
            return fallbackText;
        }

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String result = response.getResult().getOutput().getContent();
            if (result == null || result.isBlank()) {
                return fallbackText;
            }
            log.debug("AI 텍스트 생성 성공");
            return result;
        } catch (Exception e) {
            log.warn("AI 텍스트 생성 실패: {}", e.getMessage());
            return fallbackText;
        }
    }

    // AI 응답을 Map<String, Object>로 파싱, 실패 시 빈 맵 반환
    public Map<String, Object> getResponseMap(String response) {
        try {
            if (response == null || response.isBlank()) {
                return Map.of();
            }
            String cleanJson = cleanJsonResponse(response);
            if (cleanJson.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패: {}", e.getMessage());
            return Map.of(); // 빈 맵 반환
        }
    }

    // ```json ... ``` 또는 불필요한 전후 텍스트 제거 + 첫 { ~ 마지막 } 까지 슬라이싱
    private String cleanJsonResponse(String response) {
        String s = response.trim();
        // 코드펜스 제거
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            int last = s.lastIndexOf("```");
            if (first >= 0 && last > first) {
                s = s.substring(first + 1, last).trim();
            }
        }
        // 첫 '{'와 마지막 '}' 사이만 추출
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return s.substring(start, end + 1).trim();
        }
        return "";
    }
}
