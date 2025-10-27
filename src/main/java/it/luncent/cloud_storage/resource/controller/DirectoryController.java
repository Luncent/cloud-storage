package it.luncent.cloud_storage.resource.controller;

import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import it.luncent.cloud_storage.resource.validation.IsDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceMetadataResponse>> getDirectoryContent(@RequestParam
                                                                              @Validated
                                                                              @IsDirectory String path) {
        return ResponseEntity.ok(resourceService.getDirectoryContents(path));
    }

    @PostMapping
    public ResponseEntity<ResourceMetadataResponse> createEmptyDirectory(@RequestParam
                                                                         @Validated
                                                                         @IsDirectory String path){
        return ResponseEntity.ok(resourceService.createEmptyDirectory(path));
    }

}
