package it.luncent.cloud_storage.resource.directory.controller;

import it.luncent.cloud_storage.resource.directory.service.DirectoryService;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
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

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<List<ResourceMetadataResponse>> getDirectoryContent(@RequestParam
                                                                              @Validated
                                                                              @IsDirectory String path) {
        //TODO не фильтрую зарезервированные имена файлов
        return ResponseEntity.ok(directoryService.getContents(path));
    }

    @PostMapping
    public ResponseEntity<ResourceMetadataResponse> createEmptyDirectory(@RequestParam
                                                                         @Validated
                                                                         @IsDirectory String path) {
        return ResponseEntity.ok(directoryService.createEmptyDirectory(path));
    }

}
