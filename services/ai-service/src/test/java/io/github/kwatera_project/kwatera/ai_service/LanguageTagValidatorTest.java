package io.github.kwatera_project.kwatera.ai_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTagValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageTagValidatorTest {
  private final LanguageTagValidator validator = new LanguageTagValidator();

  @ParameterizedTest
  @ValueSource(
      strings = {"en", "EN-us", "zh-Hant-TW", "pt-BR", "es-419", "de-1996", "en-x-private"})
  void acceptsLanguageTagsWithSupportedSubtags(String value) {
    assertThat(validator.isValid(value, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        " ",
        "en_US",
        "en-",
        "en--US",
        "en-abcdefghi",
        "en-a",
        "en-a-foo-b",
        "und",
        "x-private"
      })
  void rejectsMalformedAndUndefinedLanguageTags(String value) {
    assertThat(validator.isValid(value, null)).isFalse();
  }

  @Test
  void preservesLengthLimitAndRejectsLargeRepeatedInputs() {
    String longestTag = "en-x-" + "a-".repeat(28) + "ab";
    assertThat(longestTag).hasSize(63);
    assertThat(validator.isValid(longestTag, null)).isTrue();
    assertThat(validator.isValid(longestTag + "c", null)).isFalse();
    assertThat(validator.isValid("en-" + "a-".repeat(50000) + "!", null)).isFalse();
  }

  @Test
  void preservesNullHandlingAndSourceOnlyAutoDetection() {
    assertThat(validator.isValid(null, null)).isTrue();
    assertThat(validator.isValid("auto", null)).isFalse();
    var annotation = mock(LanguageTag.class);
    when(annotation.allowAuto()).thenReturn(true);
    validator.initialize(annotation);
    assertThat(validator.isValid("auto", null)).isTrue();
    assertThat(validator.isValid("AUTO", null)).isTrue();
  }
}
