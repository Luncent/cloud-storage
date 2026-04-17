package it.luncent.cloud_storage.resource.utils;

import it.luncent.cloud_storage.security.model.User;
import org.apache.tika.utils.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.DIRECTORY_SUFFIX;

public class PathUtils {

    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";

    public static String getAbsolutePath(String relativePath) {
        Long currentUserId = getCurrentUser().getId();

        if (!StringUtils.isEmpty(relativePath)) {
            //почемуто фронт на запрос удаления присылает путь который начинается с / а во всех других случаях такого нет
            if (relativePath.startsWith(DIRECTORY_SUFFIX)) {
                relativePath = relativePath.substring(DIRECTORY_SUFFIX.length());
            }
        }

        return StringUtils.isBlank(relativePath) || relativePath.equals(DIRECTORY_SUFFIX)
                ? String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "")
                : String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, relativePath);
    }

    public static String getRelativePath(String absolutePath) {
        Long currentUserId = getCurrentUser().getId();
        String userContextPrefix = String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "");
        return absolutePath.substring(userContextPrefix.length());
    }

    public static String getDirectoryObjectNewPath(String oldPath, String fromDir, String toDir) {
        return toDir + (oldPath.substring(fromDir.length()));
    }

    private static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
