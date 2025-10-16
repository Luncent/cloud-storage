package it.luncent.cloud_storage.resource.util;

import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourcePathUtil {

    private static final String USER_RESOURCE_PATH_TEMPLATE = "user-%d-files/%s";

    private final AuthService authService;

    @Value("${minio.users-bucket}")
    private String usersBucket;

    public ResourcePath getResourcePathFromRelative(String relativePath) {
        Long currentUserId = authService.getCurrentUser().id();
        String realPath = relativePath.equals("/")
                ? String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, "")
                : String.format(USER_RESOURCE_PATH_TEMPLATE, currentUserId, relativePath);
        return new ResourcePath(relativePath, realPath, usersBucket);
    }

    public ResourcePath getResourcePathFromAbsolute(String absolutePath) {
        return new ResourcePath(null, absolutePath, usersBucket);
    }
}
