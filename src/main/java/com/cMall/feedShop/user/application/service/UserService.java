//package com.cMall.feedShop.user.application.service;
//
//import com.cMall.feedShop.user.application.dto.request.UserLoginRequest;
//import com.cMall.feedShop.user.application.dto.request.UserSignUpRequest;
//import com.cMall.feedShop.user.application.dto.response.AuthTokenResponse;
//import com.cMall.feedShop.user.application.dto.response.UserResponse;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@Transactional
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtProvider jwtProvider; // JWT 토큰 발급용 (직접 구현/주입 필요)
//
//    public UserResponse signUp(UserSignUpRequest request) {
//         1. 중복 체크
//        if (userRepository.existsByUsername(request.getUsername())) {
//            throw new RuntimeException("이미 존재하는 사용자입니다.");
//        }
//         2. 비밀번호 암호화
//        String encodedPassword = passwordEncoder.encode(request.getPassword());
//
//         3. 사용자 생성 및 저장
//        User user = new User(
//                request.getUsername(),
//                encodedPassword,
//                UserStatus.ACTIVE,
//                UserRole.ROLE_USER
//        );
//        userRepository.save(user);
//
//         4. UserResponse로 변환해서 반환
//        return UserResponse.from(user);
//    }
//
//    public AuthTokenResponse login(UserLoginRequest request) {
//         1. 사용자 검증
//        User user = userRepository.findByUsername(request.getUsername())
//                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
//
//         2. 비밀번호 확인
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
//        }
//
//         3. JWT 토큰 생성
//        String accessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole().name());
//        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());
//
//         4. 토큰 반환
//        return new AuthTokenResponse(accessToken, refreshToken);
//    }
//}
