package it.luncent.cloud_storage.security.controllers;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import it.luncent.cloud_storage.user.model.UserResponse;
import it.luncent.cloud_storage.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    //TODO add validation
    @PostMapping("/sign-up")
    public ResponseEntity<AuthenticationResponse> singUp(@RequestBody RegistrationRequest registrationRequest,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response){
         UserDetails userDetails = userService.register(registrationRequest);
         Authentication authentication = new UsernamePasswordAuthenticationToken(
                 userDetails.getUsername(),
                 null,
                 userDetails.getAuthorities()
         );
         SecurityContext context = SecurityContextHolder.getContext();
         context.setAuthentication(authentication);
         securityContextRepository.saveContext(context, request, response);
         AuthenticationResponse authenticationResponse = new AuthenticationResponse(authentication.getName());
         return new ResponseEntity<>(authenticationResponse, HttpStatus.CREATED);
    }
}
