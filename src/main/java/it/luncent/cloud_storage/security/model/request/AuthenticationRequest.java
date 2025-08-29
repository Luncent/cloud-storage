package it.luncent.cloud_storage.security.model.request;

public record AuthenticationRequest (String username,
                                     String password) {
}
