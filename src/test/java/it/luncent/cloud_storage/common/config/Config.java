package it.luncent.cloud_storage.common.config;

import it.luncent.cloud_storage.user.mapper.UserMapperImpl;
import it.luncent.cloud_storage.user.service.UserServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@TestConfiguration
@Import({
        UserServiceImpl.class,
        UserMapperImpl.class,
        BCryptPasswordEncoder.class
})
public class Config {
}
