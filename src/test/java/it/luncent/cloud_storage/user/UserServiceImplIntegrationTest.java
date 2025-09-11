package it.luncent.cloud_storage.user;

import it.luncent.cloud_storage.config.TestConfig;
import it.luncent.cloud_storage.config.UserConfig;
import it.luncent.cloud_storage.config.integration.IntegrationTest;
import it.luncent.cloud_storage.security.exception.UsernameExistsException;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.service.UserServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static it.luncent.cloud_storage.user.test_data.UserTestData.createRegistrationRequest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserServiceImplIntegrationTest extends IntegrationTest {
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private EntityManager entityManager;

    @Test
    void saveNotExistingUserLeadsToUserCreation(){
        RegistrationRequest request = createRegistrationRequest();
        userServiceImpl.create(request);
        assertDoesNotThrow(()-> userServiceImpl.loadUserByUsername(request.username()));
    }

    @Test
    void saveExistingUserLeadsToException(){
        RegistrationRequest request = createRegistrationRequest();
        userServiceImpl.create(request);
        assertThrows(UsernameExistsException.class, ()-> userServiceImpl.create(request));
    }
}
