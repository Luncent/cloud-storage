package it.luncent.cloud_storage.resource.model.common;

public record Path (String directory, String file) {
    public String getFullPath() {
        return directory + "/" + file;
    }
}
