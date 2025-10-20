package it.luncent.cloud_storage.storage.service;

import io.minio.messages.Item;
import it.luncent.cloud_storage.resource.exception.DownloadException;
import it.luncent.cloud_storage.resource.model.common.ResourcePath;
import it.luncent.cloud_storage.resource.util.ResourcePathUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.luncent.cloud_storage.storage.util.StorageUtil.isDirectory;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final StorageService storageService;
    private final ResourcePathUtil resourcePathUtil;
    private final ExecutorService executor;

    //TODO add check for existence
    @Override
    public void downloadArchive(ResourcePath zippedDirectoryPath, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            List<Item> objects = new ArrayList<>();
            storageService.populateWithDirectoryObjects(zippedDirectoryPath, objects);
            for (Item item : objects) {

                String absolutePath = item.objectName();
                ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(absolutePath);

                ZipEntry zipEntry = new ZipEntry(getResourceRelativePath(absolutePath, zippedDirectoryPath.absolute()));
                zipOutputStream.putNextEntry(zipEntry);
                if (isDirectory(absolutePath)) {
                    zipOutputStream.closeEntry();
                    continue;
                }

                InputStream inputStream = storageService.downloadFile(resourcePath);
                writeFileInputStreamToOutputStream(inputStream, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        } catch (Exception e) {
            throw new DownloadException(e.getMessage(), e);
        }
    }

    public void downloadArchiveAsync(ResourcePath zippedDirectoryPath, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            List<Item> objects = new ArrayList<>();
            storageService.populateWithDirectoryObjects(zippedDirectoryPath, objects);
            LinkedList<ResourcePath> downloadingFilesPaths = new LinkedList<>();
            for (Item item : objects) {

                String absolutePath = item.objectName();
                ResourcePath resourcePath = resourcePathUtil.getResourcePathFromAbsolute(absolutePath);
                if (isDirectory(absolutePath)) {
                    ZipEntry zipEntry = new ZipEntry(getResourceRelativePath(absolutePath, zippedDirectoryPath.absolute()));
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.closeEntry();
                    continue;
                }
                downloadingFilesPaths.add(resourcePath);
            }

            CountDownLatch countDownLatch = new CountDownLatch(downloadingFilesPaths.size());
            List<GetFileInputStreamCallable> getFilesInputStreamsTasks = downloadingFilesPaths.stream()
                    .map(fileToDownload -> new GetFileInputStreamCallable(fileToDownload, countDownLatch))
                    .toList();
            List<Future<InputStream>> filesInputStreamsFutures = executor.invokeAll(getFilesInputStreamsTasks);
            countDownLatch.await();

            for(Future<InputStream> futureInputStream : filesInputStreamsFutures) {
                ZipEntry zipEntry = new ZipEntry(getResourceRelativePath(downloadingFilesPaths.pop().absolute(), zippedDirectoryPath.absolute()));
                zipOutputStream.putNextEntry(zipEntry);
                writeFileInputStreamToOutputStream(futureInputStream.get(), zipOutputStream);
                zipOutputStream.closeEntry();
            }
        } catch (Exception e) {
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

    private String getResourceRelativePath(String absolute, String zippedFolderPath) {
        return absolute.replace(zippedFolderPath, "");
    }

    @AllArgsConstructor
    private class GetFileInputStreamCallable implements Callable<InputStream> {

        private final ResourcePath resourcePath;
        private final CountDownLatch countDownLatch;

        @Override
        public InputStream call() {
            try {
                return storageService.downloadFile(resourcePath);
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
