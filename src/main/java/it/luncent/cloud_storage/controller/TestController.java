package it.luncent.cloud_storage.controller;

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
        return "free";
    }
}
