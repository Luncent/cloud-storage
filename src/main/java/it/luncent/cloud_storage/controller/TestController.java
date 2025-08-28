package it.luncent.cloud_storage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/auth/sign-in")
    public String signIn() {
        return "Sign in";
    }

    @GetMapping("/api/auth/sign-up")
    public String singUp() {
        return "Sign un";
    }
}
