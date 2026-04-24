package it.luncent.cloud_storage.resource.directory.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DirectoryValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DirectoryPath {

    String message() default "invalid name";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
