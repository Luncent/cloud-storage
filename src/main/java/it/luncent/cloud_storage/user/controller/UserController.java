package it.luncent.cloud_storage.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Пользователи")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "получение текущего пользователя")
    public ResponseEntity<UserResponse> getUser() {
        UserResponse userResponse = new UserResponse(authService.getCurrentUser().getUsername());
        return ResponseEntity.ok(userResponse);
    }

}
