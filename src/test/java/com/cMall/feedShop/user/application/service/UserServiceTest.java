package com.cMall.feedShop.user.application.service;

import com.cMall.feedShop.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        // @SpringBootTest 어노테이션이 붙어있으면 컨텍스트 로딩 자체가 테스트의 목적이 됩니다.
        // 추가적으로, 주입받은 빈들이 null이 아닌지 확인하여 컨텍스트가 올바르게 작동하는지 검증할 수 있습니다.
        assertNotNull(userService, "UserService 빈이 주입되지 않았습니다.");
        assertNotNull(userRepository, "UserRepository 빈이 주입되지 않았습니다.");
        System.out.println("Spring Boot context loaded successfully!");
    }

    @Test
    void addSimpleUser_test() {
        // given
        String loginId = "test";
        String password = "test1234";
        String email = "test@example.com";
        String phone = "010-1111-2222";

        // when
        userService.addSimpleUser(loginId, password, email, phone);

        // then
        assertTrue(userRepository.existsByLoginId(loginId));
    }
}
