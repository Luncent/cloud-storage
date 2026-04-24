package it.luncent.cloud_storage.resource.directory.validation;

import it.luncent.cloud_storage.resource.utils.PathUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.utils.StringUtils;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.FILE_NAME_PATTERN;

public class DirectoryValidator implements ConstraintValidator<DirectoryPath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StringUtils.isBlank(value) ||
               (FILE_NAME_PATTERN.matcher(value).matches() && PathUtils.isDirectory(value));
    }
}
