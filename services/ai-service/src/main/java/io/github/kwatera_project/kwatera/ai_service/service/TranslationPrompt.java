package io.github.kwatera_project.kwatera.ai_service.service;

import java.util.Locale;

public final class TranslationPrompt {
  private TranslationPrompt() {}

  // Ollama's documented TranslateGemma text template; two blank lines before the payload.
  // No system message, JSON envelope, or JSON output constraint belongs in this format.
  public static String gemma(String source, String target, String text) {
    String sourceName = Locale.forLanguageTag(source).getDisplayLanguage(Locale.ENGLISH);
    String targetName = Locale.forLanguageTag(target).getDisplayLanguage(Locale.ENGLISH);
    return "You are a professional "
        + sourceName
        + " ("
        + source
        + ") to "
        + targetName
        + " ("
        + target
        + ") translator. Your goal is to accurately convey the meaning and nuances of the original "
        + sourceName
        + " text while adhering to "
        + targetName
        + " grammar, vocabulary, and cultural sensitivities.\nProduce only the "
        + targetName
        + " translation, without any additional explanations or commentary. Please translate the following "
        + sourceName
        + " text into "
        + targetName
        + ":\n\n\n"
        + text;
  }
}
