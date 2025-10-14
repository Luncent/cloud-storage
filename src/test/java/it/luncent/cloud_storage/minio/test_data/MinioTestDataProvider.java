
package it.luncent.cloud_storage.minio.test_data;

import it.luncent.cloud_storage.storage.model.request.UploadRequest;
import it.luncent.cloud_storage.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.luncent.cloud_storage.minio.test_data.MinioConstants.ARCHIVE_PATH;
import static it.luncent.cloud_storage.minio.test_data.MinioConstants.DIRECTORY_TO_ARCHIVE;
import static it.luncent.cloud_storage.minio.test_data.MinioConstants.TEST_TARGET_DIRECTORY;

//@Component
public class MinioTestDataProvider {

/*    @Autowired
    private StorageService storageService;*/

    /*//first call prefix is uploading folderName
    public static void uploadDirectoryMain(String targetDirectory, String prefix, Path folder) {
        try (DirectoryStream<Path> directoryResources = Files.newDirectoryStream(folder)) {
            for (Path resource : directoryResources) {
                if (isDirectory(resource)) {
                    String newPrefix = prefix + resource.getFileName().toString() + "/";
                    uploadDirectoryMain(targetDirectory, newPrefix, resource);
                    continue;
                }
                uploadFile(resource.toFile(), targetDirectory, prefix);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void uploadFile(File file, String targetDirectory, String prefix) throws Exception {
        String contentType = tika.detect(file);
        String fileName = file.getName();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
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

    public static void fillMinio2() {
        Path folderPath = Path.of("src/test/resources/minio_test_data/folder1/");
        uploadDirectoryMain(TEST_USER_DIRECTORY + TEST_TARGET_DIRECTORY, TEST_COPING_DIRECTORY_NAME, folderPath);
    }

    public static void cleanMinio() {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(TEST_USER_DIRECTORY)
                            .build()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public static void fillMinio() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("folder1/", new FileInputStream(ARCHIVE_PATH));
        UploadRequest uploadRequest = new UploadRequest(TEST_TARGET_DIRECTORY, multipartFile);
        resourceService.upload(uploadRequest);
    }*/

    public static void fillTestData(StorageService storageService) throws IOException {
        createZipArchive();
        storageService.upload(createUploadRequest());
    }

    private static UploadRequest createUploadRequest() throws IOException {
        MultipartFile multipartFile = new MockMultipartFile("folder1/", new FileInputStream(ARCHIVE_PATH));
        return new UploadRequest(TEST_TARGET_DIRECTORY, multipartFile);
    }

    private static void createZipArchive() {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(ARCHIVE_PATH))) {
            File directoryToArchive = new File(DIRECTORY_TO_ARCHIVE);
            addResourceToArchive(directoryToArchive, directoryToArchive.getName(), zipOutputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static void addResourceToArchive(File resource, String resourceName, ZipOutputStream zipOutputStream) throws IOException {
        if (resource.isDirectory()) {
            ZipEntry directory = new ZipEntry(resourceName + "/");
            zipOutputStream.putNextEntry(directory);
            zipOutputStream.closeEntry();

            for (File embeddedResource : resource.listFiles()) {
                addResourceToArchive(embeddedResource, resourceName + "/" + embeddedResource.getName(), zipOutputStream);
            }
            return;
        }
        ZipEntry fileEntry = new ZipEntry(resourceName);
        zipOutputStream.putNextEntry(fileEntry);
        writeFileToArchive(resource, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private static void writeFileToArchive(File resource, ZipOutputStream zipOutputStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(resource))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, read);
            }
        }
    }
}

