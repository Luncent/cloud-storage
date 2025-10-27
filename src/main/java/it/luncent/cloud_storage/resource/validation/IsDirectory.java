package it.luncent.cloud_storage.resource.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DirectoryValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsDirectory {

    String message() default "requested path is not a directory";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
