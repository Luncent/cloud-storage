package it.luncent.cloud_storage.storage.util;

public class StorageUtil {

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}
