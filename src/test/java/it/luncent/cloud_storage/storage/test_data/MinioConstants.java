package it.luncent.cloud_storage.storage.test_data;

public class MinioConstants {
    public static final String TARGET_DIRECTORY = "test_folder/";
    public static final String EXISTING_DIRECTORY = String.format("%sfolder1/nested1/", TARGET_DIRECTORY);
    public static final String NOT_EXISTING_DIRECTORY = String.format("%sfolder2/", TARGET_DIRECTORY);
    public static final String EXISTING_FILE = String.format("%sfolder1/nested2/gggg.html", TARGET_DIRECTORY);
    public static final String NOT_EXISTING_FILE = String.format("%sfolder2/notexisting.txt", TARGET_DIRECTORY);
    public static final String TEST_COPING_DIRECTORY_NAME = "folder1/";

    public static final String DIRECTORY_TO_ARCHIVE = "src/test/resources/minio_test_data/folder1/";
    public static final String ARCHIVE_PATH = "src/test/resources/minio_test_data/folder1.zip";
}
