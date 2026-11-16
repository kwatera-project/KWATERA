package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.service.ReviewService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

  private final ReviewService reviewService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public List<ReviewResponseDto> getAllReviews(
      @RequestParam(value = "startDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(value = "endDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(value = "dateType", defaultValue = "createdAt") String dateType,
      Authentication authentication) {
    boolean isAdmin =
        authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    return reviewService.getReviewsForAdmin(startDate, endDate, dateType);
  }
}
