package it.luncent.cloud_storage.resource.validation;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isDirectory;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isMarker;

public class MoveRequestValidator implements ConstraintValidator<MoveRequestValidation, MoveRequest> {

    private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("[1-9/a-zA-Zа-яА-Я_]*");

    @Override
    public boolean isValid(MoveRequest value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String from = value.from();
        String to = value.to();
        if (isDirectory(from)) {
            Matcher matcher = DIRECTORY_NAME_PATTERN.matcher(to);
            if (!matcher.matches()) {
                context.buildConstraintViolationWithTemplate("new name has incorrect symbols").addConstraintViolation();
                return false;
            }
        }
        if ((isDirectory(from) && !isDirectory(to)) || (!isDirectory(from) && isDirectory(to))) {
            context.buildConstraintViolationWithTemplate("you cant rename file to folder or vise versa").addConstraintViolation();
            return false;
        }
        if (isMarker(from) || isMarker(to)) {
            context.buildConstraintViolationWithTemplate(EMPTY_DIRECTORY_MARKER + " name is reserved").addConstraintViolation();
            return false;
        }
        return true;
    }
}
