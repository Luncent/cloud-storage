package it.luncent.cloud_storage.resource.constants;

import java.util.regex.Pattern;

public final class ObjectStorageConstants {

    public static final String EMPTY_DIRECTORY_MARKER = "empty-folder-tag";
    public static final String DIRECTORY_SUFFIX = "/";
    public static final Pattern FILE_NAME_PATTERN = Pattern.compile("^(?!.*(?:\\.\\.|//))[^#?%]*$");

}
