package it.luncent.cloud_storage.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.luncent.cloud_storage.security.mapper.AuthMapper;
import it.luncent.cloud_storage.security.model.UserModel;
import it.luncent.cloud_storage.security.model.request.AuthenticationRequest;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.request.UsernamePasswordModel;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import it.luncent.cloud_storage.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final ObjectMapper objectMapper;

    @Override
    public AuthenticationResponse signIn(AuthenticationRequest authRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        return authenticate(request, response, authRequest);
    }

    @Override
    public AuthenticationResponse signUp(RegistrationRequest registrationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        userService.create(registrationRequest);
        return authenticate(request, response, registrationRequest);
    }

    @Override
    public void signOut(HttpServletRequest request,
                        HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(null);
        securityContextRepository.saveContext(context, request, response);
    }

    @Override
    public UserModel getCurrentUser() {
        return (UserModel) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @SneakyThrows
    private AuthenticationResponse authenticate(HttpServletRequest request, HttpServletResponse response, UsernamePasswordModel user) {
        Authentication authentication = authMapper.mapToAuthentication(user);
        authentication = authenticationManager.authenticate(authentication);
        UserModel userModel = objectMapper.readValue(authentication.getName(), UserModel.class);
        authentication = new UsernamePasswordAuthenticationToken(userModel, authentication.getCredentials(), authentication.getAuthorities());
        persistSecurityContext(authentication, request, response);
        return authMapper.mapToResponse(userModel);
    }

    private void persistSecurityContext(Authentication authentication,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
    }
}
