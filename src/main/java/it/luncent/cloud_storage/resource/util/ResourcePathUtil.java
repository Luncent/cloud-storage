package it.luncent.cloud_storage.resource.util;

import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.DIRECTORY_SUFFIX;

@Component
@RequiredArgsConstructor
public class ResourcePathUtil {

    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";

    private final AuthService authService;

    @Value("${minio.users-bucket}")
    private String usersBucket;

    public ResourcePath getResourcePathFromRelative(String relativePath) {
        String realPath = getAbsolutePathFromRelative(relativePath);
        return new ResourcePath(relativePath, realPath, usersBucket);
    }

    public String getAbsolutePathFromRelative(String relativePath) {
        Long currentUserId = authService.getCurrentUser().id();

        if(!StringUtils.isEmpty(relativePath)) {
            //почемуто фронт на запрос удаления присылает путь который начинается со / а во всех других случаях такого нет
            if(relativePath.startsWith(DIRECTORY_SUFFIX)) {
                relativePath = relativePath.substring(DIRECTORY_SUFFIX.length());
            }
        }

        return StringUtils.isBlank(relativePath) || relativePath.equals(DIRECTORY_SUFFIX)
                ? String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "")
                : String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, relativePath);
    }

    public ResourcePath getResourcePathFromAbsolute(String absolutePath) {
        return new ResourcePath(getRelativePath(absolutePath), absolutePath, usersBucket);
    }

    public ResourcePath concurrentGetPathFromAbsolute(String absolutePath, Long userId) {
        return new ResourcePath(getRelativePathConcurrent(absolutePath, userId), absolutePath, usersBucket);
    }

    public String getRelativePath(String absolutePath) {
        Long currentUserId = authService.getCurrentUser().id();
        String userContextPrefix = String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "");
        return absolutePath.substring(userContextPrefix.length());
    }

    public String getRelativePathConcurrent(String absolutePath, Long userId) {
        String userContextPrefix = String.format(USER_RESOURCE_PATH_TEMPLATE, userId, "");
        return absolutePath.substring(userContextPrefix.length());
    }
}
