package it.luncent.cloud_storage.user.controller;

import it.luncent.cloud_storage.security.service.AuthService;
import it.luncent.cloud_storage.user.model.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUser() {
        UserResponse userResponse = new UserResponse(authService.getCurrentUser().username());
        return ResponseEntity.ok(userResponse);
    }

}
