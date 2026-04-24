package it.luncent.cloud_storage.resource.archive.service;

import it.luncent.cloud_storage.resource.archive.exception.ArchiveDownloadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.luncent.cloud_storage.resource.utils.PathUtils.isDirectory;


@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    @Override
    public void outputArchive(Map<String, InputStream> resourceNamesStreams, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, InputStream> resourceNameStream : resourceNamesStreams.entrySet()) {
                String name = resourceNameStream.getKey();
                ZipEntry zipEntry = new ZipEntry(name);
                zipOutputStream.putNextEntry(zipEntry);
                if (isDirectory(name)) {
                    zipOutputStream.closeEntry();
                    continue;
                }
                InputStream inputStream = resourceNameStream.getValue();
                writeFileInputStreamToOutputStream(inputStream, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new ArchiveDownloadException(e.getMessage(), e);
        }
    }

    private void writeFileInputStreamToOutputStream(InputStream fileInputStream,
                                                    OutputStream outputStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream)) {
            //cant close OutputStream here. It is closing higher
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bis.transferTo(bufferedOutputStream);
            //zipOutputStream.closeEntry(); doesnt flush bytes left in buffer, have to do it here
            bufferedOutputStream.flush();
        }
    }
}
