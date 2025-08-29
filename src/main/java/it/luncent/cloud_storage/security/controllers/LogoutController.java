package it.luncent.cloud_storage.security.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/sign-out")
public class LogoutController {

    @PostMapping
    public ResponseEntity<?> logout() {
        return ResponseEntity.noContent().build();
    }

}
