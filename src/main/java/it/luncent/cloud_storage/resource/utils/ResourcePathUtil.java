package it.luncent.cloud_storage.resource.utils;

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
        String realPath = getAbsolutePath(relativePath);
        return new ResourcePath(relativePath, realPath, usersBucket);
    }

    public String getAbsolutePath(String relativePath) {
        Long currentUserId = authService.getCurrentUser().getId();

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

    public String getRelativePath(String absolutePath) {
        Long currentUserId = authService.getCurrentUser().getId();
        String userContextPrefix = String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "");
        return absolutePath.substring(userContextPrefix.length());
    }

    public String getFullTargetPath(String sourceFullPath, String from, String to) {
        String sourcePathPrefix = getAbsolutePath(from);
        String targetPathPrefix = getAbsolutePath(to);
        return targetPathPrefix + (sourceFullPath.substring(sourcePathPrefix.length()));
    }
}
