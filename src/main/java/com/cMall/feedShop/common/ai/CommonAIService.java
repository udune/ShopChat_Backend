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

    // AI 모델을 사용하여 텍스트 생성, 실패 시 폴백 텍스트 반환
    public String generateText(String prompt, String fallbackText) {
        if (!aiEnabled || chatModel == null) {
            log.info("AI 비활성화 상태 (aiEnabled: {}, chatModel: {}), 폴백 반환",
                    aiEnabled, chatModel != null ? "있음" : "없음");
            return fallbackText;
        }

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String result = response.getResult().getOutput().getContent();
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
            String cleanJson = cleanJsonResponse(response);
            return objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패: {}", e.getMessage());
            return Map.of(); // 빈 맵 반환
        }
    }

    // 응답에서 JSON 부분만 추출
    private String cleanJsonResponse(String response) {
        if (response.contains("{")) {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            return response.substring(start, end);
        }
        return response;
    }
}
