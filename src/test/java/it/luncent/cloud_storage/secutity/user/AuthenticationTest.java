package it.luncent.cloud_storage.secutity.user;

import it.luncent.cloud_storage.common.TestContainerBase;
import it.luncent.cloud_storage.security.controllers.AuthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
public class AuthenticationTest extends TestContainerBase {

    @Autowired
    private MockMvc mvc;

    @MockitoBean

}
