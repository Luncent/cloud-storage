package it.luncent.cloud_storage.common;

import it.luncent.cloud_storage.config.MinioClientConfig;
import it.luncent.cloud_storage.minio.service.MinioServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        MinioClientConfig.class,
        MinioServiceImpl.class,
        MappersConfig.class
})
public class MinioConfig {
}
