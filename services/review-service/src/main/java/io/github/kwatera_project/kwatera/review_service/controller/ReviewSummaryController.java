package io.github.kwatera_project.kwatera.review_service.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient.SummaryResponse;
import io.github.kwatera_project.kwatera.review_service.model.ReviewSummary;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewSummaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

@RestController
@RequestMapping("/api/reviews")
@ApiResponse(responseCode = "200", description = "Cached AI review summary")
@ApiResponse(responseCode = "204", description = "No cached summary for this accommodation")
public class ReviewSummaryController {

  private final ReviewSummaryRepository summaries;
  private final ObjectReader summaryReader;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring-managed repository dependency")
  public ReviewSummaryController(ReviewSummaryRepository summaries, ObjectMapper mapper) {
    this.summaries = summaries;
    this.summaryReader = mapper.readerFor(SummaryResponse.class);
  }

  private String validateLanguage(String language) {
    String normalized = language.toLowerCase(Locale.ROOT);
    if (!normalized.equals("en") && !normalized.equals("pl")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported summary language");
    }
    return normalized;
  }

  @GetMapping("/units/{unitId}/summary")
  @Operation(summary = "Get the cached AI review summary for a unit")
  public ResponseEntity<SummaryResponse> forUnit(
      @PathVariable("unitId") UUID unitId,
      @RequestParam(name = "language", defaultValue = "en") String language) {
    return response(summaries.findByUnitIdAndLanguage(unitId, validateLanguage(language)));
  }

  @GetMapping("/properties/{propertyId}/summary")
  @Operation(summary = "Get the cached AI review summary for a property")
  public ResponseEntity<SummaryResponse> forProperty(
      @PathVariable("propertyId") UUID propertyId,
      @RequestParam(name = "language", defaultValue = "en") String language) {
    return response(summaries.findByPropertyIdAndLanguage(propertyId, validateLanguage(language)));
  }

  private ResponseEntity<SummaryResponse> response(Optional<ReviewSummary> cached) {
    if (cached.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(summaryReader.readValue(cached.get().getAiSummary()));
  }
}
