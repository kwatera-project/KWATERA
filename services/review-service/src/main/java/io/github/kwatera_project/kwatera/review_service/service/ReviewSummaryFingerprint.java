package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ReviewSummaryFingerprint {

  private ReviewSummaryFingerprint() {}

  public static String calculate(List<ReviewSummaryInput> inputs) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }

    List<ReviewSummaryInput> sortedInputs =
        inputs.stream().sorted(Comparator.comparing(ReviewSummaryInput::id)).toList();

    for (ReviewSummaryInput input : sortedInputs) {
      String entry = input.id() + "|" + input.updatedAt() + "\n";
      digest.update(entry.getBytes(StandardCharsets.UTF_8));
    }

    return HexFormat.of().formatHex(digest.digest());
  }
}
