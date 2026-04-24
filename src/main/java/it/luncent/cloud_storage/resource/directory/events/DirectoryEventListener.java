package it.luncent.cloud_storage.resource.directory.events;

import it.luncent.cloud_storage.common.events.registration.RegistrationEvent;
import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectoryEventListener implements ApplicationListener<RegistrationEvent> {

    private final DirectoryService directoryService;

    @Override
    public void onApplicationEvent(@NotNull RegistrationEvent event) {
        directoryService.createEmptyDirectory(PathUtils.getUserPathPrefix());
    }
}
