package it.luncent.cloud_storage.resource.validation;

import it.luncent.cloud_storage.resource.model.request.MoveRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static it.luncent.cloud_storage.common.constants.ObjectStorageConstants.EMPTY_DIRECTORY_MARKER;
import static it.luncent.cloud_storage.common.util.ObjectStorageUtil.isMarker;

public class MoveRequestValidator implements ConstraintValidator<MoveRequestValidation, MoveRequest> {
    @Override
    public boolean isValid(MoveRequest value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String from = value.from();
        String to = value.to();
        if ((from.endsWith("/") && !to.endsWith("/")) || (!from.endsWith("/") && to.endsWith("/"))) {
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
