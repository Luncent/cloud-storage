package it.luncent.cloud_storage.config;

import it.luncent.cloud_storage.storage.service.StorageServiceImpl;
import it.luncent.cloud_storage.storage.test_data.MinioTestDataProvider;
import it.luncent.cloud_storage.resource.mapper.ResourceMapperImpl;
import it.luncent.cloud_storage.resource.service.ResourceServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        ResourceServiceImpl.class,
        ResourceMapperImpl.class,
        StorageServiceImpl.class,
        // MinioTestDataRepositoryTest.class
        MinioTestDataProvider.class,
        MinioConfig.class,
        StorageServiceImpl.class,
})
public class MinioTestConfig {

}
