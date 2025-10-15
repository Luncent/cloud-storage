package it.luncent.cloud_storage.storage;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import it.luncent.cloud_storage.storage.test_data.MinioTestDataProvider;
import it.luncent.cloud_storage.security.model.UserModel;
import it.luncent.cloud_storage.security.service.AuthServiceImpl;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
public class MinioTests {

    @Value("${minio.users-bucket}")
    private String bucket;
    @MockitoSpyBean
    private AuthServiceImpl authService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private Tika tika;

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

    @Test
    void testCheckExistence() {
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix("not existing")
                        .build()
        );
        assertThat(objects).hasSize(1);
    }

    @Test
    void testGetFileResourceData() {
        ResourceMetadataResponse metadataResponse = resourceService.getResourceMetadata("diplom/антиплагиатd.pdf");
        assertThat(metadataResponse.size()).isNotNull();
        assertThat(metadataResponse.path().endsWith("/")).isFalse();
    }

    @Test
    void testGetFolderResourceData() {
        ResourceMetadataResponse metadataResponse = resourceService.getResourceMetadata("diplom/");
        assertThat(metadataResponse.size()).isNull();
        assertThat(metadataResponse.path().endsWith("/")).isTrue();
    }


    /*@Test
    void testBucketCreation(){
        storageService.createBucketForUsersData();
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
        List<ResourceMetadataResponse> uploadedResources = storageService.upload(uploadRequest);
        for(ResourceMetadataResponse resource : uploadedResources){
            assertThat(storageService.getResourceMetadata(resource.path())).isNotNull();
        }
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
        ResourceMetadataResponse response = storageService.getResourceMetadata("second");

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
    }*/
}
