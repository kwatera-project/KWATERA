package io.github.kwatera_project.kwatera.ai_service.service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import io.github.kwatera_project.kwatera.ai_service.config.AiProperties;
import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTagValidator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalLanguageDetection {
  // The long booking sample retained its meaning when Qwen supplied the regional source tag.
  private static final int MAX_FAST_PATH_CHARACTERS = 256;
  private static final int MIN_LETTERS = 18;
  private static final double MESSAGE_MARGIN = 0.15;
  private static final double FRAGMENT_MARGIN = 0.12;
  private static final Pattern LINKS = Pattern.compile("(?i)(?:https?://|www\\.)\\S+");
  private static final Pattern CLAUSE_BOUNDARY = Pattern.compile("[.!?,;:\\r\\n。？！、，；：]+");
  private static final Pattern WORD = Pattern.compile("\\p{L}[\\p{L}\\p{M}]*");
  // Score all Lingua languages; use only the calibrated winners for the optimization.
  private static final Set<Language> CALIBRATED =
      Set.of(
          Language.ENGLISH,
          Language.POLISH,
          Language.GERMAN,
          Language.FRENCH,
          Language.UKRAINIAN,
          Language.JAPANESE);

  private final LanguageDetector detector;

  public LocalLanguageDetection(AiProperties properties) {
    detector =
        properties.languageDetectionEnabled()
                && properties.translationFormat() == AiProperties.TranslationFormat.TRANSLATE_GEMMA
            ? LanguageDetectorBuilder.fromAllLanguages().withLowAccuracyMode().build()
            : null;
  }

  public Optional<String> detect(String text) {
    if (detector == null || text.length() > MAX_FAST_PATH_CHARACTERS) return Optional.empty();
    String evidence = LINKS.matcher(text).replaceAll(" ");
    if (evidence.codePoints().filter(Character::isLetter).count() < MIN_LETTERS) {
      return Optional.empty();
    }
    Language language = winner(evidence, MESSAGE_MARGIN);
    if (!CALIBRATED.contains(language)
        || (language != Language.JAPANESE && WORD.matcher(evidence).results().limit(4).count() < 4)
        || hasConflictingFragments(evidence, language)) {
      return Optional.empty();
    }
    String tag = language.getIsoCode639_1().name().toLowerCase(Locale.ROOT);
    return Optional.of(LanguageTagValidator.canonical(tag));
  }

  private boolean hasConflictingFragments(String evidence, Language language) {
    for (String clause : CLAUSE_BOUNDARY.split(evidence)) {
      if (conflicts(clause, language)) return true;

      // Also look for a language change within an unpunctuated word sequence.
      var words = WORD.matcher(clause).results().toList();
      if (words.size() >= 8) {
        int midpoint = words.get(words.size() / 2).start();
        if (conflicts(clause.substring(0, midpoint), language)
            || conflicts(clause.substring(midpoint), language)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean conflicts(String fragment, Language language) {
    if (fragment.codePoints().filter(Character::isLetter).count() < 10) return false;
    Language candidate = winner(fragment, FRAGMENT_MARGIN);
    return candidate != Language.UNKNOWN && candidate != language;
  }

  private Language winner(String text, double minimumMargin) {
    var scores = detector.computeLanguageConfidenceValues(text);
    if (scores.isEmpty()) return Language.UNKNOWN;
    double runnerUp = scores.values().stream().skip(1).findFirst().orElse(0.0);
    return scores.get(scores.firstKey()) - runnerUp >= minimumMargin
        ? scores.firstKey()
        : Language.UNKNOWN;
  }
}
