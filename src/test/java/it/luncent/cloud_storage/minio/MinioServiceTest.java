package it.luncent.cloud_storage.minio;

import io.minio.MinioClient;
import it.luncent.cloud_storage.config.MinioTestConfig;
import it.luncent.cloud_storage.minio.exception.MinioException;
import it.luncent.cloud_storage.minio.service.MinioService;
import it.luncent.cloud_storage.minio.test_data.MinioTestDataProvider;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static it.luncent.cloud_storage.minio.test_data.MinioConstants.BUCKET_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(
        classes = {MinioTestConfig.class},
        initializers = ConfigDataApplicationContextInitializer.class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MinioServiceTest {

    @Autowired
    private MinioService minioService;
    @Autowired
    private MinioTestDataProvider minioTestDataProvider;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private Tika tika;

    /*@AfterAll
    public void cleanUp() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.removeBucket(RemoveBucketArgs.builder()
                .bucket(BUCKET_NAME)
                .build());
    }*/

    @Test
    void createBucket(){
        minioTestDataProvider.createZipArchive();
        minioService.createBucket(BUCKET_NAME);
        assertThat(minioService.bucketExists(BUCKET_NAME)).isTrue();
    }

    @Test
    void createEmptyDirectory(){
        minioService.createEmptyDirectory(BUCKET_NAME, "/lv1/lvl2/");
        assertThat(minioService.getObject(BUCKET_NAME, "/lv1/lvl2/"+"empty-folder-tag")).isNotNull();
    }

    @Test
    void uploadFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file),contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();
    }

    @Test
    void deleteFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file),contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();

        minioService.deleteFile(BUCKET_NAME, file.getName());
        assertThrows(MinioException.class, ()-> minioService.getObject(BUCKET_NAME, file.getName()));

    }

    @Test
    void downloadFile() throws IOException {
        File file = new File("src/test/resources/minio_test_data/folder1/nested2/gggg.html");
        String contentType = tika.detect(file);
        minioService.uploadFile(BUCKET_NAME, file.getName(), new FileInputStream(file),contentType);
        assertThat(minioService.getObject(BUCKET_NAME, file.getName())).isNotNull();

        try(BufferedInputStream bis = new BufferedInputStream(minioService.downloadFile(BUCKET_NAME, "gggg.html"));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("src/test/resources/downloadedfile.html"))
        ){
            byte[] buffer = new byte[1024];
            Integer readedBytes;
            while ((readedBytes = bis.read(buffer)) != -1) {
                bos.write(buffer,0, readedBytes);
            }
        }

        minioService.deleteFile(BUCKET_NAME, file.getName());
        assertThrows(MinioException.class, ()-> minioService.getObject(BUCKET_NAME, file.getName()));
    }

   /* boolean bucketExists(String bucketName);

    StatObjectResponse getObject(String bucketName, String objectName);

    void uploadFile(String bucketName, String fileName, InputStream inputStream, String contentType);

    void deleteFile(String bucketName, String fileName);

    void downloadFile(String bucketName, String fileName, OutputStream outputStream);

    void createEmptyDirectory(String bucketName, String folderName);*/

}
