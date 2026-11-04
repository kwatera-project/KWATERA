package io.github.kwatera_project.kwatera.ai_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LanguageTagValidator.class)
public @interface LanguageTag {
  String message() default "Use a valid BCP 47 language tag.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  boolean allowAuto() default false;
}
