package it.luncent.cloud_storage.security.service;

import it.luncent.cloud_storage.security.mapper.AuthMapper;
import it.luncent.cloud_storage.security.model.request.AuthenticationRequest;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import it.luncent.cloud_storage.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    @Override
    public AuthenticationResponse signIn(AuthenticationRequest authRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        Authentication authentication = authMapper.mapToAuthentication(authRequest);
        authentication = authenticationManager.authenticate(authentication);
        persistSecurityContext(authentication, request, response);
        return authMapper.mapToResponse(authentication);
    }

    @Override
    public AuthenticationResponse signUp(RegistrationRequest registrationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        userService.create(registrationRequest);
        Authentication authentication = authMapper.mapToAuthentication(registrationRequest);
        authentication = authenticationManager.authenticate(authentication);
        persistSecurityContext(authentication, request, response);
        return authMapper.mapToResponse(authentication);
    }

    @Override
    public void signOut(HttpServletRequest request,
                        HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(null);
        securityContextRepository.saveContext(context, request, response);
    }

    @Override
    public Authentication getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    //TODO create Custom Authentication to store user id, create common interface with getName() getPassword() for registerDto and loginDto
    private <T> AuthenticationResponse authenticate(HttpServletRequest request, HttpServletResponse response, T ){

    }

    private void persistSecurityContext(Authentication authentication,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
    }
}
