package it.luncent.cloud_storage.storage.config;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.apache.tika.Tika;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties, MinioLoggingInterceptor minioLoggingInterceptor) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(minioLoggingInterceptor)
                .build();

        return MinioClient.builder()
                .endpoint(minioProperties.endpoint())
                .credentials(minioProperties.username(), minioProperties.password())
                .httpClient(httpClient)
                .build();
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
