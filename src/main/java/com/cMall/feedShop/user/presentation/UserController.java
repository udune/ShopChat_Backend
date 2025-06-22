package com.cMall.feedShop.user.presentation;

import com.cMall.feedShop.user.application.dto.request.UserSignUpRequest;
import com.cMall.feedShop.user.application.dto.response.UserProfileResponse; // UserProfileResponse 임포트
import com.cMall.feedShop.user.application.service.UserProfileService;
import com.cMall.feedShop.user.application.service.UserService; // UserService 임포트 (이름 충돌 방지를 위해 UserApplicationService 등으로 변경 고려)
import lombok.RequiredArgsConstructor; // Lombok 임포트
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // final 필드를 인자로 받는 생성자를 자동 생성
public class UserController {
    private final UserProfileService userProfileService;
    private final UserService userService; // UserService 주입

    // 기존 @Autowired 생성자 삭제 (lombok @RequiredArgsConstructor가 대신함)
    // public UserController(UserProfileService userProfileService) {
    //     this.userProfileService = userProfileService;
    // }

    // 사용자 프로필을 조회하는 예시 메서드
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getUserProfile(@PathVariable Long userId) {
        // 주입받은 userProfileService를 사용하여 실제 비즈니스 로직 호출
        UserProfileResponse response = userProfileService.getUserProfile(userId);
        return response;
    }

    @PostMapping("/signup")
    public String signUp(@RequestBody UserSignUpRequest request) {
        System.out.println("Received password (after AOP): " + request.getPassword());

        // PasswordEncryptionAspect에 의해 이미 암호화된 DTO를 서비스 계층으로 전달
        userService.signUp(request); // UserService 호출 (메서드 이름을 signup으로 가정)

        return "User signed up successfully";
    }

    // JWT 로그인 메서드도 있다면 여기에 추가
    // @PostMapping("/login")
    // public AuthTokenResponse login(@RequestBody UserLoginRequest request) {
    //     return userService.login(request);
    // }
}