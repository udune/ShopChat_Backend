package com.cMall.feedShop.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @Column(name="name")
    private String name;

    @Column(name="nickname")
    private String nickname;

    @Column(nullable = false, length = 20)
    private String phone;

    public UserProfile(User user, String name, String nickname, String phone) {
        this.user = user;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        if (user != null) {
            user.setUserProfile(this); // 양방향 관계 설정
        }
    }
}
