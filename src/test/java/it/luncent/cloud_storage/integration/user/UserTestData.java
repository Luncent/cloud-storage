package it.luncent.cloud_storage.integration.user;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import org.instancio.Instancio;

public class UserTestData {

    public static RegistrationRequest createRegistrationRequest() {
        String userName = null;
        do {
            userName = Instancio.create(String.class);
        }while (userName.length()<5 || userName.length()>20);
        String password = Instancio.create(String.class);
        return new RegistrationRequest(userName,password,password);
    }
}
