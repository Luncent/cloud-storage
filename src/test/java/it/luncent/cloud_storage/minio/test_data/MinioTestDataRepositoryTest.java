package it.luncent.cloud_storage.minio.test_data;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import it.luncent.cloud_storage.common.config.MinioConfig;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.service.MinioService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static it.luncent.cloud_storage.common.constants.MinioConstants.*;
import static java.nio.file.Files.isDirectory;

@Component
@SpringJUnitConfig(
        classes = {MinioConfig.class},
        initializers = ConfigDataApplicationContextInitializer.class
)
public class MinioTestDataRepositoryTest {

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private Tika tika;
    @Autowired
    private MinioService minioService;

    private static final String MINIO_TEST_DATA_DIRECTORY = "src/test/resources/minio_test_data/folder1/";
    private static final String ARCHIVE_PATH = "src/test/resources/minio_test_data/folder1.zip";

    //first call prefix is uploading folderName
    public void uploadDirectoryMain(String targetDirectory, String prefix, Path folder){
        try(DirectoryStream<Path> directoryResources = Files.newDirectoryStream(folder)){
            for(Path resource : directoryResources){
                if(isDirectory(resource)){
                    String newPrefix = prefix + resource.getFileName().toString()+"/";
                    uploadDirectoryMain(targetDirectory, newPrefix, resource);
                    continue;
                }
                uploadFile(resource.toFile(), targetDirectory, prefix);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void uploadFile(File file, String targetDirectory, String prefix) throws Exception {
        String contentType = tika.detect(file);
        String fileName = file.getName();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))){
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .contentType(contentType)
                            .object(targetDirectory + prefix + fileName)
                            .stream(bis, file.length(), -1)
                            .build()
            );
        }
    }

    public void fillMinio2(){
        Path folderPath = Path.of("src/test/resources/minio_test_data/folder1/");
        uploadDirectoryMain(TEST_USER_DIRECTORY + TEST_TARGET_DIRECTORY, TEST_COPING_DIRECTORY_NAME, folderPath);
    }
    public void cleanMinio() {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(TEST_USER_DIRECTORY)
                            .build()
            );
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void fillMinio() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("folder1/", new FileInputStream(ARCHIVE_PATH));
        UploadRequest uploadRequest = new UploadRequest(TEST_TARGET_DIRECTORY, multipartFile);
        minioService.upload(uploadRequest);
    }

    @Test
    public void createZipArchive() {
        //MINIO_TEST_DATA_DIRECTORY
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(ARCHIVE_PATH))){

            File targetDirectory = new File(MINIO_TEST_DATA_DIRECTORY);
            addResourceToArchive(targetDirectory, targetDirectory.getName(), zipOutputStream);

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void addResourceToArchive(File resource, String resourceName, ZipOutputStream zipOutputStream) throws IOException {
        if(resource.isDirectory()){
            ZipEntry directory = new ZipEntry(resourceName+"/");
            zipOutputStream.putNextEntry(directory);
            zipOutputStream.closeEntry();

            for(File embeddedResource : resource.listFiles()){
                addResourceToArchive(embeddedResource, resourceName+"/"+embeddedResource.getName(), zipOutputStream);
            }
            return;
        }
        ZipEntry fileEntry = new ZipEntry(resourceName);
        zipOutputStream.putNextEntry(fileEntry);
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(resource))) {
            byte[] buffer = new byte[1024];
            int read;
            while((read = bis.read(buffer)) != -1){
                zipOutputStream.write(buffer, 0, read);
            }
            zipOutputStream.closeEntry();
        }
    }
}
