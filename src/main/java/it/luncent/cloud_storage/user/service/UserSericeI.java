package it.luncent.cloud_storage.user.service;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.entity.User;
import it.luncent.cloud_storage.user.model.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserSericeI {

    UserDetails register(RegistrationRequest registrationRequest);

}
