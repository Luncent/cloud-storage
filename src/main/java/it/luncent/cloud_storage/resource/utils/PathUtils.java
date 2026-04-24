package it.luncent.cloud_storage.resource.utils;

import it.luncent.cloud_storage.security.model.User;
import org.apache.tika.utils.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.DIRECTORY_SUFFIX;
import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;

public class PathUtils {

    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";

    public static String getAbsolutePath(String path) {
        if (!StringUtils.isEmpty(path)) {
            //почемуто фронт на запрос удаления присылает путь который начинается с / а во всех других случаях такого нет
            if (path.startsWith(DIRECTORY_SUFFIX)) {
                path = path.substring(DIRECTORY_SUFFIX.length());
            }
        }

        return StringUtils.isBlank(path) || path.equals(DIRECTORY_SUFFIX)
                ? getPathWithUserPrefix("")
                : getPathWithUserPrefix(path);
    }

    public static String getUserPathPrefix() {
        Long currentUserId = getCurrentUser().getId();
        return String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "");
    }

    public static String getRelativePath(String absolutePath) {
        String userContextPrefix = getPathWithUserPrefix("");
        return absolutePath.substring(userContextPrefix.length());
    }

    public static String getFileName(String absolutePath) {
        int lastSlashIndex = absolutePath.lastIndexOf('/');
        return absolutePath.substring(lastSlashIndex + 1);
    }

    public static String getDirectoryName(String absolutePath) {
        String relativePath = PathUtils.getRelativePath(absolutePath);
        if (isRootDirectory(relativePath)) {
            return DIRECTORY_SUFFIX;
        } else {
            int relativeLength = relativePath.length();
            int relativePenultimateLength = relativePath.lastIndexOf('/', relativeLength - 2);
            return relativePath.substring(relativePenultimateLength + 1);
        }
    }

    public static boolean isMarker(String objectPath) {
        int lastSlashIndex = objectPath.lastIndexOf(DIRECTORY_SUFFIX);
        if (lastSlashIndex == -1) {
            return objectPath.equals(EMPTY_DIRECTORY_MARKER);
        }
        return objectPath.substring(lastSlashIndex + 1).equals(EMPTY_DIRECTORY_MARKER);
    }

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    private static boolean isRootDirectory(String path) {
        return org.apache.commons.lang3.StringUtils.isBlank(path);
    }

    private static String getPathWithUserPrefix(String path) {
        Long currentUserId = getCurrentUser().getId();
        return String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, path);
    }

    private static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
