package it.luncent.cloud_storage.storage.model.common;

import it.luncent.cloud_storage.storage.model.request.UploadRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public record UploadingFile(String fileName,
                            String contentType,
                            String targetDirectory,
                            InputStream inputStream,
                            Optional<Long> fileSize) {

    public static UploadingFile withKnownFileSize(UploadRequest request) throws IOException {
        MultipartFile fileToUpload =  request.file();
        return new UploadingFile(
                fileToUpload.getName(),
                fileToUpload.getContentType(),
                request.targetDirectory(),
                fileToUpload.getInputStream(),
                Optional.of(fileToUpload.getSize())
        );
    }

    public static UploadingFile withUnKnownFileSize(String fileName, String contentType, String targetDirectory, InputStream inputStream) throws IOException {
        return new UploadingFile(
                fileName,
                contentType,
                targetDirectory,
                inputStream,
                Optional.empty()
        );
    }

}
