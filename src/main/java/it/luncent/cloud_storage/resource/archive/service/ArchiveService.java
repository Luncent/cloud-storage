package it.luncent.cloud_storage.resource.archive.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface ArchiveService {
    void outputArchive(Map<String, InputStream> resourceNamesStreams, OutputStream outputStream);
}
