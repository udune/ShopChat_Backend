package com.cMall.feedShop;

import com.cMall.feedShop.user.application.service.UserProfileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class FeedShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedShopApplication.class, args);
    }

    // 개발 초기 테스트용: AOP 로그 동작 확인
    @Bean
    public CommandLineRunner run(UserProfileService userProfileService) {
        return args -> {
            // AOP 로그 테스트용 메서드 호출
            userProfileService.updateUserProfile(456L, "new data");
        };
    }
}
