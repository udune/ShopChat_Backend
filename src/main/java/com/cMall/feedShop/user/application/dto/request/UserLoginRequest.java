package com.cMall.feedShop.user.application.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
@Getter
public class UserLoginRequest {
//    private String loginId;
    private String email;

    private String password;
}
