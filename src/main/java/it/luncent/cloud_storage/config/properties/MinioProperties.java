package it.luncent.cloud_storage.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties (String endpoint,
                               String username,
                               String password){}
