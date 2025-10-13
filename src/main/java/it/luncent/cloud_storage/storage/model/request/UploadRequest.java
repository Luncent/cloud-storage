package it.luncent.cloud_storage.storage.model.request;

import org.springframework.web.multipart.MultipartFile;

public record UploadRequest(String targetDirectory,
                            MultipartFile file) {
}
