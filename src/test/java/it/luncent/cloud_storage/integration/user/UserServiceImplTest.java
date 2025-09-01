package it.luncent.cloud_storage.integration.user;

import it.luncent.cloud_storage.common.IT;
import it.luncent.cloud_storage.common.TestContainerBase;
import it.luncent.cloud_storage.security.exception.UsernameExistsException;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.service.UserServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static it.luncent.cloud_storage.integration.user.UserTestData.createRegistrationRequest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IT
public class UserServiceImplTest extends TestContainerBase {
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private EntityManager entityManager;

    @Test
    void saveNotExistingUserLeadsToUserCreation(){
        RegistrationRequest request = createRegistrationRequest();
        userServiceImpl.create(request);
        entityManager.flush();
        entityManager.clear();
        assertDoesNotThrow(()-> userServiceImpl.loadUserByUsername(request.username()));
    }

    @Test
    void saveExistingUserLeadsToException(){
        RegistrationRequest request = createRegistrationRequest();
        userServiceImpl.create(request);
        entityManager.flush();
        entityManager.clear();
        assertThrows(UsernameExistsException.class, ()-> userServiceImpl.create(request));
    }
}
