package io.github.kwatera_project.kwatera.ai_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.IllformedLocaleException;
import java.util.Locale;

public class LanguageTagValidator implements ConstraintValidator<LanguageTag, String> {
  private boolean allowAuto;

  @Override
  public void initialize(LanguageTag annotation) {
    allowAuto = annotation.allowAuto();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true; // @NotBlank handles required fields.
    if ("auto".equalsIgnoreCase(value)) return allowAuto;
    if (value.length() > 63 || !value.matches("[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*")) return false;
    try {
      String language = new Locale.Builder().setLanguageTag(value).build().getLanguage();
      return !language.isEmpty() && !"und".equals(language);
    } catch (IllformedLocaleException exception) {
      return false;
    }
  }

  public static String canonical(String value) {
    return new Locale.Builder().setLanguageTag(value).build().toLanguageTag();
  }
}
