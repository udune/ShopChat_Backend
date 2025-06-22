package com.cMall.feedShop.user.application.dto.request;

import com.cMall.feedShop.annotation.CustomEncryption;
import lombok.Getter;

@Getter
public class UserSignUpRequest {
    private String username; // ERD의 login_id에 해당
    @CustomEncryption // 비밀번호 암호화를 위한 어노테이션
    private String password;
    private String email;
    private String phone;
}
