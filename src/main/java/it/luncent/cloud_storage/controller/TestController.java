package it.luncent.cloud_storage.controller;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/protected")
    public String protectedMethod() {
        return "protected";
    }

    @GetMapping("/free")
    public String freeMethod() {
        SecurityContext context = SecurityContextHolder.getContext();
        return "free";
    }
}
