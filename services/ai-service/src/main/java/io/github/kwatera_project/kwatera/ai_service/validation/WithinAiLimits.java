package io.github.kwatera_project.kwatera.ai_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AiInputLimitsValidator.class)
public @interface WithinAiLimits {
  String message() default "Input exceeds configured limits.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
