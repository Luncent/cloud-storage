package it.luncent.cloud_storage.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@TestConfiguration
@ComponentScan(basePackages = "it.luncent.cloud_storage.config")
public class TestConfig {
}
