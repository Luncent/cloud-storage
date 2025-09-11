package it.luncent.cloud_storage.config;

import it.luncent.cloud_storage.minio.mapper.MinioMapperImpl;
import it.luncent.cloud_storage.minio.service.MinioServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        //ResourceServiceImpl.class,
        MinioMapperImpl.class,
        MinioServiceImpl.class,
        // MinioTestDataRepositoryTest.class
        MinioConfig.class,
        MinioServiceImpl.class,
})
public class MinioTestConfig {

}
