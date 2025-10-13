package it.luncent.cloud_storage.security.controllers;

import it.luncent.cloud_storage.security.model.request.AuthenticationRequest;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import it.luncent.cloud_storage.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticationResponse> signIn(@RequestBody AuthenticationRequest authNRequest,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response){
        return ResponseEntity.ok(authService.signIn(authNRequest, request, response));
    }

    @PostMapping("/sign-up")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResponseEntity<AuthenticationResponse> signUp(@RequestBody @Validated RegistrationRequest registrationRequest,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response){
        return new ResponseEntity(authService.signUp(registrationRequest, request, response), HttpStatus.CREATED);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(HttpServletRequest request,
                                     HttpServletResponse response){
        authService.signOut(request, response);
        return ResponseEntity.noContent().build();
    }

}
