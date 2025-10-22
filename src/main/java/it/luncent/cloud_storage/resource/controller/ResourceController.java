package it.luncent.cloud_storage.resource.controller;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;
import it.luncent.cloud_storage.resource.service.ResourceService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
                                                   String path){
        resourceService.deleteResource(path);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceMetadataResponse> moveResource(@Validated MoveRequest moveRequest) {
        return ResponseEntity.ok(resourceService.moveResource(moveRequest));
    }
}
