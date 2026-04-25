package it.luncent.cloud_storage.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.tika.Tika;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@Slf4j
public class MinioConfig {

    @Bean
    @Profile("dev")
    public MinioClient devMinioClient(MinioProperties minioProperties, MinioLoggingInterceptor minioLoggingInterceptor) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(minioLoggingInterceptor)
                .build();
        return createMinioClient(minioProperties, httpClient);
    }

    @Bean
    @Profile("!dev")
    public MinioClient minioClient(MinioProperties minioProperties) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .build();
        return createMinioClient(minioProperties, httpClient);
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }

    private MinioClient createMinioClient(MinioProperties minioProperties, OkHttpClient httpClient) {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.endpoint())
                .credentials(minioProperties.username(), minioProperties.password())
                .httpClient(httpClient)
                .build();
        String usersBucket = minioProperties.usersBucket();
        createBucketIfNotExists(client, usersBucket);
        return client;
    }

    private void createBucketIfNotExists(MinioClient client, String bucket) {
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(bucket)
                .build();
        try {
            if (client.bucketExists(bucketExistsArgs)) {
                log.debug("Bucket {} exists", bucket);
                return;
            }
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build();
            client.makeBucket(makeBucketArgs);
            log.debug("Bucket {} created", bucket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
