package it.luncent.cloud_storage.security.model.request;

import jakarta.validation.constraints.Size;

public record AuthenticationRequest (@Size(min=5, max = 25, message = "username must be in range from 5 to 25") String username,
                                     String password) {
}
