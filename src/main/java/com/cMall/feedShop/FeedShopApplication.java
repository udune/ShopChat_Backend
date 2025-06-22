package com.cMall.feedShop;

import com.cMall.feedShop.user.application.service.UserProfileService; // 서비스 임포트
import org.springframework.boot.CommandLineRunner; // 애플리케이션 시작 시 코드 실행
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // @Bean 어노테이션 임포트
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class FeedShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedShopApplication.class, args);
	}

	// 애플리케이션 시작 후 특정 코드 실행 (테스트 목적)
	@Bean
	public CommandLineRunner run(UserProfileService userProfileService) {
		return args -> {
			System.out.println("--- 테스트 시작: LoggingAspect 확인 ---");

			// 2. 파라미터가 없는 메서드 호출 테스트 (만약 있다면)
			// userProfileService.someOtherMethod();

			// 3. 예외 발생 메서드 호출 테스트 (주석 해제 후 테스트)
			try {
				userProfileService.updateUserProfile(456L, "new data");
				//userProfileService.updateUserProfile(456L, "trigger error"); // UserProfileService에서 예외를 던지도록 수정 후 테스트
			} catch (Exception e) {
				System.err.println("Exception caught in main app: " + e.getMessage());
			}

			System.out.println("--- 테스트 종료 ---");
		};
	}
}