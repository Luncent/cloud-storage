package it.luncent.cloud_storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MinioTests {

    static MinioClient minioClient;

    @BeforeAll
    static void setUp() {
        minioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("admin", "12345678")
                .build();
    }

    @Test
    public void createBucket() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket("second")
                .build());
    }

    @Test
    void getResourceMetadata() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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
    void renameFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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


    private void moveDir(String bucket, String fromFolder, String toFolder, String prefix) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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
                String fullFileName = object.objectName();
                int fullPathLength = fullFileName.length();
                String filename = fullFileName.substring(fromFolder.length(), fullPathLength);
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucket)
                                .object(toFolder + filename)
                                .source(
                                        CopySource.builder()
                                                .bucket(bucket)
                                                .object(fullFileName)
                                                .build()
                                )
                                .build()
                )

                System.out.println(object.objectName());
            }
        }

        for(Item directory : directories){
            moveDir(bucket, directory.objectName());
        }

    }

    @Test
    void renameDirectory() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {

            printFolderContent("newbucket", "nested1/");

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
