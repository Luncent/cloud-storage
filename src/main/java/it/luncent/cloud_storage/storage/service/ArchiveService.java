package it.luncent.cloud_storage.storage.service;

import it.luncent.cloud_storage.resource.model.common.ResourcePath;

import java.io.OutputStream;

public interface ArchiveService {

    void downloadArchive(ResourcePath path, OutputStream outputStream);

}
