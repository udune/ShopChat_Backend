package com.cMall.feedShop.user.domain.repository;

import com.cMall.feedShop.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    // 사용자 이름으로 User 엔티티를 찾는 메서드 (UserService에서 사용)
    Optional<User> findByUsername(String username);
}
