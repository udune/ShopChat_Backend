package com.cMall.feedShop.user.presentation;

import com.cMall.feedShop.user.application.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserProfileService userProfileService;

    @Autowired
    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }
}
