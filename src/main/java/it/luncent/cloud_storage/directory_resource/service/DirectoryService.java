package it.luncent.cloud_storage.directory_resource.service;

import it.luncent.cloud_storage.resource.model.response.ResourceMetadataResponse;

import java.util.List;

public interface DirectoryService {

    List<ResourceMetadataResponse> getDirectoryContents(String path);

    ResourceMetadataResponse createEmptyDirectory(String path);
}
