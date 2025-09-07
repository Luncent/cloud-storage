package it.luncent.cloud_storage.common.config;

import it.luncent.cloud_storage.minio.mapper.MinioMapperImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({MinioMapperImpl.class})
public class MappersConfig {
}
