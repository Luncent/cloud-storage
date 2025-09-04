package it.luncent.cloud_storage.config;

import io.minio.MinioClient;
import it.luncent.cloud_storage.config.properties.MinioProperties;
import org.apache.tika.Tika;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties.endpoint())
                .credentials(minioProperties.username(), minioProperties.password())
                .build();
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
