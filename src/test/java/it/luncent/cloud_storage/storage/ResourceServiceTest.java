package it.luncent.cloud_storage.storage;

import io.minio.MinioClient;
import it.luncent.cloud_storage.storage.exception.ResourceNotFoundException;
import it.luncent.cloud_storage.security.model.UserModel;
import it.luncent.cloud_storage.security.service.AuthServiceImpl;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import it.luncent.cloud_storage.storage.test_data.MinioTestDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static it.luncent.cloud_storage.storage.test_data.MinioConstants.EXISTING_DIRECTORY;
import static it.luncent.cloud_storage.storage.test_data.MinioConstants.EXISTING_FILE;
import static it.luncent.cloud_storage.storage.test_data.MinioConstants.NOT_EXISTING_DIRECTORY;
import static it.luncent.cloud_storage.storage.test_data.MinioConstants.NOT_EXISTING_FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

/*@SpringJUnitConfig(
        classes = {MinioTestConfig.class},
        initializers = ConfigDataApplicationContextInitializer.class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)*/
@SpringBootTest
public class ResourceServiceTest {

    @Value("${minio.users-bucket}")
    private String bucket;
    @MockitoSpyBean
    private AuthServiceImpl authService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private ResourceService resourceService;

    @BeforeEach
    void fill() throws Exception {
        UserModel userModel = new UserModel(1L, "oleg");
        doReturn(userModel).when(authService).getCurrentUser();
        MinioTestDataProvider.fillTestData(resourceService, minioClient, bucket);
    }

    @AfterEach
    void clean() throws Exception {
        MinioTestDataProvider.cleanMinio(minioClient, bucket);
    }

    @Nested
    class GetResourceMetadataTest {

        @Test
        void shouldReturnDirectoryMetadataWithoutFileSize(){
            ResourceMetadataResponse resourceMetadata = resourceService.getResourceMetadata(EXISTING_DIRECTORY);
            assertThat(resourceMetadata.size()).isNull();
        }

        @Test
        void shouldThrowNotFoundErrorWhenDirectoryResourceDoesNotExist() {
            assertThrows(ResourceNotFoundException.class, ()-> resourceService.getResourceMetadata(NOT_EXISTING_DIRECTORY));
        }

        @Test
        void shouldReturnFileMetadataWithFileSize(){
            ResourceMetadataResponse resourceMetadata = resourceService.getResourceMetadata(EXISTING_FILE);
            assertThat(resourceMetadata.size()).isNotNull();
        }

        @Test
        void shouldThrowNotFoundErrorWhenFileResourceDoesNotExist() {
            assertThrows(ResourceNotFoundException.class, ()-> resourceService.getResourceMetadata(NOT_EXISTING_FILE));
        }
    }



    /*@Test
    void createEmptyDirectory() {
        minioService.createEmptyDirectory(BUCKET_NAME, "/lv1/lvl2/");
        assertThat(minioService.getObject(BUCKET_NAME, "/lv1/lvl2/" + "empty-folder-tag")).isNotNull();
    }

    @Test
    void uploadFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file), contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();
    }

    @Test
    void deleteFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file), contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();

        minioService.deleteFile(BUCKET_NAME, file.getName());
        assertThrows(MinioException.class, () -> minioService.getObject(BUCKET_NAME, file.getName()));

    }

    @Test
    void downloadFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file), contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();

        try (BufferedInputStream bis = new BufferedInputStream(minioService.downloadFile(BUCKET_NAME, "gggg.html"));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("src/test/resources/downloadedfile.html"))
        ) {
            byte[] buffer = new byte[1024];
            Integer readedBytes;
            while ((readedBytes = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, readedBytes);
            }
        }

        minioService.deleteFile(BUCKET_NAME, file.getName());
        assertThrows(MinioException.class, () -> minioService.getObject(BUCKET_NAME, file.getName()));
    }*/

   /* boolean bucketExists(String bucketName);

    StatObjectResponse getObject(String bucketName, String objectName);

    void uploadFile(String bucketName, String relativePath, InputStream inputStream, String contentType);

    void deleteFile(String bucketName, String relativePath);

    void downloadFile(String bucketName, String relativePath, OutputStream outputStream);

    void createEmptyDirectory(String bucketName, String folderName);*/

}
