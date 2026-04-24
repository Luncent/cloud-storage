package it.luncent.cloud_storage.resource.validation;

import it.luncent.cloud_storage.resource.utils.PathUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;

import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static it.luncent.cloud_storage.resource.constants.ObjectStorageConstants.FILE_NAME_PATTERN;
import static it.luncent.cloud_storage.resource.utils.PathUtils.isDirectory;

public class MoveRequestValidator implements ConstraintValidator<MoveRequest, it.luncent.cloud_storage.resource.model.request.MoveRequest> {

    @Override
    public boolean isValid(it.luncent.cloud_storage.resource.model.request.MoveRequest value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String from = value.from();
        String to = value.to();
        Matcher nameMatcher = FILE_NAME_PATTERN.matcher(to);
        if (!nameMatcher.matches()) {
            context.buildConstraintViolationWithTemplate("new name has incorrect symbols").addConstraintViolation();
            return false;
        }
        if ((isDirectory(from) && !isDirectory(to)) || (!isDirectory(from) && isDirectory(to))) {
            context.buildConstraintViolationWithTemplate("you cant rename file to folder or vise versa").addConstraintViolation();
            return false;
        }
        if (PathUtils.isMarker(to)) {
            context.buildConstraintViolationWithTemplate(EMPTY_DIRECTORY_MARKER + " name is reserved").addConstraintViolation();
            return false;
        }
        return true;
    }
}
