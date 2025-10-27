package it.luncent.cloud_storage.resource.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.utils.StringUtils;

import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;

public class DirectoryValidator implements ConstraintValidator<IsDirectory, String> {
    @Override
    public void initialize(IsDirectory constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StringUtils.isBlank(value) || isDirectory(value);
    }
}
