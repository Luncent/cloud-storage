package it.luncent.cloud_storage.config;

import it.luncent.cloud_storage.config.properties.MinioProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@TestConfiguration
@ComponentScan(basePackages = "it.luncent.cloud_storage.config")
public class TestConfig {
}
