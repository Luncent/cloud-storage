package it.luncent.cloud_storage.resource.controller;

import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceMetadataResponse> getResourceMetadata(@RequestParam(name = "path")
                                                                        @NotNull(message = "request does not have path attribute")
                                                                        @Pattern(regexp = "[a-zA-Z_а-я-А-Я0-9]//", message = "path contains wrong symbols")
                                                                        String path) {
        return ResponseEntity.ok(resourceService.getResourceMetadata(path));
    }
}
