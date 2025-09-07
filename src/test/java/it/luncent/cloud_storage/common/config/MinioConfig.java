package it.luncent.cloud_storage.common.config;

import it.luncent.cloud_storage.minio.service.MinioServiceImpl;
import it.luncent.cloud_storage.minio.test_data.MinioTestDataRepositoryTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        it.luncent.cloud_storage.config.MinioConfig.class,
        MinioServiceImpl.class,
        MappersConfig.class,
        MinioTestDataRepositoryTest.class
})
public class MinioConfig {
}
