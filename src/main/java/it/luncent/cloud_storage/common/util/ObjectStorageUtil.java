package it.luncent.cloud_storage.common.util;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;

public class ObjectStorageUtil {

    public static boolean isMarker(String objectPath) {
        return objectPath.endsWith(EMPTY_DIRECTORY_MARKER);
    }

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}
