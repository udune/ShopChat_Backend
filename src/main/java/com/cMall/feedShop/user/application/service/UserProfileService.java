package com.cMall.feedShop.user.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    public String getUserProfile(Long userId) {
        // 실제 비즈니스 로직
        // 간단한 테스트를 위해 딜레이를 줍니다.
        try {
            Thread.sleep(500); // 0.5초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "User profile data for ID: " + userId;
    }

    public void updateUserProfile(Long userId, String newProfile) {
        // 사용자 프로필 업데이트 로직
        log.info("Updating profile for user {} to {}", userId, newProfile); // 플레이스홀더 사용 권장 (성능 및 가독성)
        // ...
        // 만약 여기서 예외를 던지면 @AfterThrowing 또는 @Around의 catch 블록이 동작하는지 확인할 수 있습니다.
        // throw new RuntimeException("Test exception during update");
    }
}