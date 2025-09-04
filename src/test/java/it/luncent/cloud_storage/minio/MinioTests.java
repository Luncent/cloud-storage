package it.luncent.cloud_storage.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import it.luncent.cloud_storage.common.MinioConfig;
import it.luncent.cloud_storage.minio.model.request.UploadRequest;
import it.luncent.cloud_storage.minio.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.minio.service.MinioService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringJUnitConfig(
        classes = {MinioConfig.class},
        initializers = ConfigDataApplicationContextInitializer.class
)
public class MinioTests {

    private static final String BUCKET_NAME = "user-files";

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioService minioService;
    @Autowired
    private Tika tika;

    @Test
    void testBucketCreation(){
        minioService.createBucketForUsersData();
    }

    //first call prefix is uploading folderName
    void uploadFolderResourceMain(String folderForSaving, String prefix, Path folder){
        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)){
            for(Path path : directoryStream){
                if(Files.isDirectory(path)){
                    String newPrefix = prefix + path.getFileName().toString()+"/";
                    uploadFolderResourceMain(folderForSaving, newPrefix, path);
                    continue;
                }
                uploadFile(path.toFile(), folderForSaving, prefix);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    void uploadFile(File file, String targetFolderPath, String prefix) throws Exception {
        String contentType = tika.detect(file);
        String fileName = file.getName();
        try (FileInputStream fis = new FileInputStream(file)){
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .contentType(contentType)
                            .object(targetFolderPath + prefix + fileName)
                            .stream(fis, file.length(), -1)
                            .build()
            );
        }
    }

    @Test
    void uploadFolderResource(){
        Path folderPath = Path.of("src/test/resources/minio_test_data/folder1/");
        String folderToUpload = "user-1-files/diplom/";
        uploadFolderResourceMain(folderToUpload, "folder1/", folderPath);
    }



    

    @Test
    void uploadFileResource(){
        File file = new File("src/test/resources/minio_test_data/folder1/file1.sql");
        try(FileInputStream fis = new FileInputStream(file)){
            String contentType = tika.detect(file);
            String folderToUpload = "user-1-files/diplom/";
            String filename = file.getName();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("user-files")
                            .object(folderToUpload+filename)
                            .contentType(contentType)
                            .stream(fis, file.length(), -1)
                            .build()
            );
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Test
    void uploadFile(){
        MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());

        UploadRequest uploadRequest = new UploadRequest(null, null);
        List<ResourceMetadataResponse> uploadedResources = minioService.upload(uploadRequest);
        for(ResourceMetadataResponse resource : uploadedResources){
            assertThat(minioService.getResourceMetadata(resource.path())).isNotNull();
        }
    }

    @Test
    void testGetFileResourceData(){
        ResourceMetadataResponse metadataResponse = minioService.getResourceMetadata("diplom/антиплагиатd.pdf");
        assertThat(metadataResponse.size()).isNotNull();
        assertThat(metadataResponse.path().endsWith("/")).isFalse();
    }

    @Test
    void testGetFolderResourceData(){
        ResourceMetadataResponse metadataResponse = minioService.getResourceMetadata("diplom/");
        assertThat(metadataResponse.size()).isNull();
        assertThat(metadataResponse.path().endsWith("/")).isTrue();
    }

    //------------------------------------------------------


    @Test
    public void createBucket() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket("second")
                .build());
    }

    @Test
    void fileResourceMetadataHasSizeField() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        ResourceMetadataResponse response = minioService.getResourceMetadata("second");

        assertThat(response.size()).isNotNull();

        StatObjectResponse objectStat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("test")
                        .object("lowercasefolder/file1.txt")
                        .build()
        );
        String filename = objectStat.object();
        Long byteSize = objectStat.size();
        System.out.println(objectStat);
    }

    @Test
    void deleteResource() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket("test")
                .object("newFolder/Новый текстовый документ.txt")
                .build());
    }

    @Test
    void moveFileToNewLocationResource() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("test")
                            .object("copy-folder/file2.txt")
                            .source(CopySource.builder()
                                    .bucket("test")
                                    .object("lowercasefolder/file1.txt")
                                    .build())
                            .build()
            );
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("test")
                            .object("lowercasefolder/file1.txt")
                            .build()
            );
        }catch (ErrorResponseException e){
            if(e.errorResponse().code().equals("NoSuchKey")){
                System.out.println("source recourse not found");
            }
            e.printStackTrace();
        }
    }

    @Test
    void renameOrMoveFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("test")
                            .object("copy-folder/file1.txt")
                            .source(CopySource.builder()
                                    .bucket("test")
                                    .object("copy-folder/file2.txt")
                                    .build())
                            .build()
            );
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("test")
                            .object("copy-folder/file2.txt")
                            .build()
            );
        }catch (ErrorResponseException e){
            if(e.errorResponse().code().equals("NoSuchKey")){
                System.out.println("source recourse not found");
            }
            e.printStackTrace();
        }
    }

    private void printFolderContent(String bucket, String prefix) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        System.out.println("-------------------DIR-----------------");

        Iterable<Result<Item>> d = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build()
        );

        List<Item> directories = new ArrayList<>();

        for (Result<Item> r : d) {
            Item object = r.get();
            if(object.isDir()){
                directories.add(object);
            }
            else{
                System.out.println(r.get().objectName());
            }
        }

        for(Item directory : directories){
            printFolderContent(bucket, directory.objectName());
        }

    }


    private void moveDir(String fromFolder, String toFolder, String prefix) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        System.out.println("-------------------DIR-----------------");

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("newbucket")
                        .prefix(prefix)
                        .build()
        );

        List<Item> directories = new ArrayList<>();
        List<DeleteObject> deleteObjects = new ArrayList<>();

        for (Result<Item> result : results) {
            Item object = result.get();
            if(object.isDir()){
                directories.add(object);
            }
            else{
                String fullFileName = object.objectName();
                int fullPathLength = fullFileName.length();
                String relativeFilename = fullFileName.substring(fromFolder.length(), fullPathLength);
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket("newbucket")
                                .object(toFolder + relativeFilename)
                                .source(
                                        CopySource.builder()
                                                .bucket("newbucket")
                                                .object(fullFileName)
                                                .build()
                                )
                                .build()
                );
                deleteObjects.add(new DeleteObject(fullFileName));

                //System.out.println(relativeFilename);
            }
        }

        Iterable<Result<DeleteError>> errorsResults = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket("newbucket")
                        .objects(deleteObjects)
                        .build()
        );

        for(Result<DeleteError> error : errorsResults){
            DeleteError deleteError = error.get();
            System.out.println(deleteError.message());
        }

        for(Item directory : directories){
            moveDir(fromFolder,toFolder, directory.objectName());
        }

    }

    @Test
    void deleteObjects(){
        List<DeleteObject> deleteObjects = new ArrayList<>();
        deleteObjects.add(new DeleteObject("newbucket" ,null));
    }

    @Test
    void renameOrMoveDirectory() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {

            moveDir("folder3/newFolder", "folder4/", "folder3/newFolder");

            /*minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("test")
                            .object("copy-folder/file1.txt")
                            .source(CopySource.builder()
                                    .bucket("test")
                                    .object("copy-folder/file2.txt")
                                    .build())
                            .build()
            );
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("test")
                            .object("copy-folder/file2.txt")
                            .build()
            );*/
        }catch (ErrorResponseException e){
            if(e.errorResponse().code().equals("NoSuchKey")){
                System.out.println("source recourse not found");
            }
            e.printStackTrace();
        }
    }
}
