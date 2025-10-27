package it.luncent.cloud_storage.resource.controller;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.request.UploadRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import it.luncent.cloud_storage.resource.validation.IsDirectory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceMetadataResponse> getResourceMetadata(@RequestParam(name = "path")
                                                                        @NotNull(message = "request does not have path attribute")
                                                                        @Pattern(regexp = "[a-zA-Z_а-я-А-Я0-9/.]+", message = "path contains wrong symbols")
                                                                        String path) {
        return ResponseEntity.ok(resourceService.getResourceMetadata(path));
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam(name = "path")
                                                                  @NotNull(message = "request does not have path attribute")
                                                                  @Pattern(regexp = "[a-zA-Z_а-я-А-Я0-9/.]+", message = "path contains wrong symbols")
                                                                  String path) {
        StreamingResponseBody responseBody = outputStream -> resourceService.downloadResource(outputStream, path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(@RequestParam(name = "path")
                                               @NotNull(message = "request does not have path attribute")
                                               @Pattern(regexp = "[a-zA-Z_а-я-А-Я0-9/.]+", message = "path contains wrong symbols")
                                               String path) {
        resourceService.deleteResource(path);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceMetadataResponse> moveResource(@Validated MoveRequest moveRequest) {
        return ResponseEntity.ok(resourceService.moveResource(moveRequest));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceMetadataResponse>> searchResource(@RequestParam(name = "query") String queryParam) {
        Optional<String> query = queryParam.isBlank() ? Optional.empty() : Optional.of(queryParam);
        return ResponseEntity.ok(resourceService.searchResource(query));
    }

    //TODO check folders creation from filename
    // add validation
    // check collisions
    // bug with uploading with params: filename: ODOS Prototype.postman_collection.json, path: r/
    @PostMapping
    public ResponseEntity<List<ResourceMetadataResponse>> uploadResource(@RequestParam MultipartFile file,
                                                                         @RequestParam String path) {
        UploadRequest uploadRequest = new UploadRequest(path, file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceService.upload(uploadRequest));
    }
}
