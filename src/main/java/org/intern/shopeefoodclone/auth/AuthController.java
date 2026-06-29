package org.intern.shopeefoodclone.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    @GetMapping("/login")
    public String login() {
        return "Login endpoint";
    }

    @PostMapping("/register")
    public String register() {
        return "Register endpoint";
    }

}
