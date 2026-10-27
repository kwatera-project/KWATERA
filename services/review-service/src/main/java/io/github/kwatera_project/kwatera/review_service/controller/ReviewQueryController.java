package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewPageDto;
import io.github.kwatera_project.kwatera.review_service.service.ReviewQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@ApiResponse(responseCode = "200", description = "Reviews and rating summary")
@ApiResponse(responseCode = "400", description = "Invalid identifier or pagination")
@ApiResponse(responseCode = "404", description = "Accommodation not found")
@ApiResponse(responseCode = "502", description = "Invalid property service response")
@ApiResponse(responseCode = "503", description = "Property service unavailable")
public class ReviewQueryController {

  private final ReviewQueryService reviewQueryService;

  public ReviewQueryController(ReviewQueryService reviewQueryService) {
    this.reviewQueryService = reviewQueryService;
  }

  @GetMapping("/units/{unitId}")
  @Operation(
      summary = "Get reviews and rating summary for a unit",
      description = "Public endpoint. Rating summary covers all reviews for the unit.")
  public ReviewPageDto getForUnit(
      @PathVariable("unitId") UUID unitId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    return reviewQueryService.getForUnit(unitId, page, size);
  }

  @GetMapping("/properties/{propertyId}")
  @Operation(
      summary = "Get reviews and rating summary for a property",
      description = "Public endpoint. Includes reviews of all units belonging to the property.")
  public ReviewPageDto getForProperty(
      @PathVariable("propertyId") UUID propertyId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    return reviewQueryService.getForProperty(propertyId, page, size);
  }
}
