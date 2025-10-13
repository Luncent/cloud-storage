package it.luncent.cloud_storage.config;

import it.luncent.cloud_storage.minio.mapper.ResourceMapperImpl;
import it.luncent.cloud_storage.minio.service.MinioServiceImpl;
import it.luncent.cloud_storage.storage.service.StorageServiceImpl;
import it.luncent.cloud_storage.minio.test_data.MinioTestDataProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        StorageServiceImpl.class,
        ResourceMapperImpl.class,
        MinioServiceImpl.class,
        // MinioTestDataRepositoryTest.class
        MinioTestDataProvider.class,
        MinioConfig.class,
        MinioServiceImpl.class,
})
public class MinioTestConfig {

}
