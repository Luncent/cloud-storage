package it.luncent.cloud_storage.resource.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.utils.StringUtils;

import java.util.regex.Matcher;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.FILE_NAME_PATTERN;

public class PathValidator implements ConstraintValidator<Path, String> {

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isBlank(path)) {
            context.buildConstraintViolationWithTemplate("path is blank").addConstraintViolation();
            return false;
        }
        Matcher nameMatcher = FILE_NAME_PATTERN.matcher(path);
        if (!nameMatcher.matches()) {
            context.buildConstraintViolationWithTemplate("invalid path").addConstraintViolation();
            return false;
        }
        return true;
    }
}
