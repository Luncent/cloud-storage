package it.luncent.cloud_storage.storage.service;

import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.luncent.cloud_storage.storage.util.StorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final StorageService storageService;
    private final ResourcePathUtil resourcePathUtil;

    //TODO add check for existence
    @Override
    public void downloadArchive(ResourcePath zippedDirectoryPath, OutputStream outputStream) {
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)){
            List<Item> objects = new ArrayList<>();
            storageService.populateWithDirectoryObjects(zippedDirectoryPath, objects);
            for (Item item : objects) {

                String absolutePath = item.objectName();
                ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(absolutePath);

                ZipEntry zipEntry = new ZipEntry(getZipEntryRelativePath(absolutePath, zippedDirectoryPath.absolute()));
                zipOutputStream.putNextEntry(zipEntry);
                if(isDirectory(absolutePath)) {
                    zipOutputStream.closeEntry();
                    continue;
                }

                InputStream inputStream = storageService.downloadFile(resourcePath);
                writeFileInputStreamToOutputStream(inputStream, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }catch (Exception e){
            throw new DownloadException(e.getMessage(), e);
        }
    }

    //TODO повторяющийся метод как и в ResourceService
    private void writeFileInputStreamToOutputStream(InputStream fileInputStream, OutputStream outputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(fileInputStream)) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bis.transferTo(bufferedOutputStream);
            bufferedOutputStream.flush();
        } catch (Exception e) {
            throw new DownloadException(e.getMessage(), e);
        }
    }

    private String getZipEntryRelativePath(String absolute, String zippedFolderPath){
        return absolute.replace(zippedFolderPath, "");
    }
}
