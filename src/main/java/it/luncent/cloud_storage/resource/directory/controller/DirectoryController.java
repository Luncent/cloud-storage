package it.luncent.cloud_storage.resource.directory.controller;

import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.directory.validation.DirectoryPath;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<List<ResourceMetadataResponse>> getDirectoryContent(@RequestParam
                                                                              @Valid
                                                                              @DirectoryPath String path) {
        return ResponseEntity.ok(directoryService.getContents(PathUtils.getAbsolutePath(path)));
    }

    @PostMapping
    public ResponseEntity<ResourceMetadataResponse> createEmptyDirectory(@RequestParam
                                                                         @Valid
                                                                         @DirectoryPath String path) {
        return ResponseEntity.ok(directoryService.createEmptyDirectory(PathUtils.getAbsolutePath(path)));
    }

}
