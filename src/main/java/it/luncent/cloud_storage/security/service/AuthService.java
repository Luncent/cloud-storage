package it.luncent.cloud_storage.security.service;

import it.luncent.cloud_storage.security.model.request.AuthenticationRequest;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthenticationResponse signIn(AuthenticationRequest authRequest,
                                  HttpServletRequest request,
                                  HttpServletResponse response);

    AuthenticationResponse signUp(RegistrationRequest registrationRequest,
                                  HttpServletRequest request,
                                  HttpServletResponse response);

    void signOut(HttpServletRequest request,
                 HttpServletResponse response);
}
