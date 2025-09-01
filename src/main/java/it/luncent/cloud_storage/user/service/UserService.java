package it.luncent.cloud_storage.user.service;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.model.UserResponse;

public interface UserService {

    UserResponse create(RegistrationRequest registrationRequest);

}
