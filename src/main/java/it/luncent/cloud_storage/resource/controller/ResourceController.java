package it.luncent.cloud_storage.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.luncent.cloud_storage.resource.directory.validation.DirectoryPath;
import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import it.luncent.cloud_storage.resource.utils.PathUtils;
import it.luncent.cloud_storage.resource.validation.Path;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Tag(name = "Ресурсы")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "получение информации о ресурсе")
    public ResponseEntity<ResourceMetadataResponse> getResourceMetadata(@RequestParam(name = "path")
                                                                        @Valid @Path String path) {
        return ResponseEntity.ok(resourceService.getMetadata(PathUtils.getAbsolutePath(path)));
    }

    @GetMapping("/download")
    @Operation(summary = "скачивание ресурса")
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam(name = "path")
                                                                  @Valid @Path String path) {
        StreamingResponseBody responseBody = outputStream -> resourceService.downloadResource(outputStream, PathUtils.getAbsolutePath(path));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @DeleteMapping
    @Operation(summary = "удаление ресурса")
    public ResponseEntity<Void> deleteResource(@RequestParam(name = "path")
                                               @Valid @Path String path) {
        resourceService.deleteResource(PathUtils.getAbsolutePath(path));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/move")
    @Operation(summary = "переименование/перемещение ресурса")
    public ResponseEntity<ResourceMetadataResponse> moveResource(@Valid
                                                                 @it.luncent.cloud_storage.resource.validation.MoveRequest
                                                                 MoveRequest moveRequest) {
        MoveRequest requestWithAbsolutePath = new MoveRequest(PathUtils.getAbsolutePath(moveRequest.from()),
                PathUtils.getAbsolutePath(moveRequest.to()));
        return ResponseEntity.ok(resourceService.moveResource(requestWithAbsolutePath));
    }

    @GetMapping("/search")
    @Operation(summary = "поиск ресурсов")
    public ResponseEntity<List<ResourceMetadataResponse>> searchResource(@RequestParam(name = "query") String queryParam) {
        return ResponseEntity.ok(resourceService.search(queryParam));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "загрузка файлов")
    public ResponseEntity<List<ResourceMetadataResponse>> uploadResource(@RequestPart(name = "object") List<MultipartFile> objects,
                                                                         @RequestPart(required = false)
                                                                         @Valid @DirectoryPath
                                                                         String path) {
        path = path == null ? "" : path;
        String absolutePath = PathUtils.getAbsolutePath(path);
        List<UploadRequest> uploadRequests = objects.stream()
                //TODO мб фронт пофиксить
                .map(file -> new UploadRequest(absolutePath, file))
                .toList();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceService.upload(uploadRequests));
    }
}
