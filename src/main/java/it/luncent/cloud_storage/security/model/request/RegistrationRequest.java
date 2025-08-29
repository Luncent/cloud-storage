package it.luncent.cloud_storage.security.model.request;

public record RegistrationRequest (String username,
                                   String password,
                                   String repeatedPassword) {
}
