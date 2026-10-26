package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.dto.UpdateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewRepository reviewRepository;

  @InjectMocks private ReviewService reviewService;

  private UUID reviewId;
  private UUID authorId;
  private UUID reservationId;
  private Review review;

  @BeforeEach
  void setUp() {
    reviewId = UUID.randomUUID();
    authorId = UUID.randomUUID();
    reservationId = UUID.randomUUID();

    review = new Review();
    review.setId(reviewId);
    review.setAuthorId(authorId);
    review.setReservationId(reservationId);
    review.setRating(5);
    review.setComment("Great place!");
    Instant fixedTime = Instant.parse("2026-01-01T12:00:00Z");
    review.setCreatedAt(fixedTime);
    review.setUpdatedAt(fixedTime);
  }

  @Test
  void createReview_ShouldSaveAndReturnReview() {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);
    request.setComment("Great place!");

    when(reviewRepository.save(any(Review.class))).thenReturn(review);

    Review created = reviewService.createReview(authorId, request);

    assertNotNull(created);
    assertEquals(authorId, created.getAuthorId());
    assertEquals(5, created.getRating());
    verify(reviewRepository, times(1)).save(any(Review.class));
  }

  @Test
  void getReviewsByAuthor_ShouldReturnListOfDtos() {
    when(reviewRepository.findReviewsByAuthorId(authorId)).thenReturn(List.of(review));

    List<ReviewResponseDto> result = reviewService.getReviewsByAuthor(authorId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(reviewId, result.get(0).getId());
    assertEquals("Great place!", result.get(0).getComment());
    verify(reviewRepository, times(1)).findReviewsByAuthorId(authorId);
  }

  @Test
  void getReviewDetails_ShouldReturnDto_WhenReviewExists() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    ReviewResponseDto result = reviewService.getReviewDetails(reviewId);

    assertNotNull(result);
    assertEquals(reviewId, result.getId());
    assertEquals(5, result.getRating());
    verify(reviewRepository, times(1)).findById(reviewId);
  }

  @Test
  void getReviewDetails_ShouldThrowNotFound_WhenReviewDoesNotExist() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              reviewService.getReviewDetails(reviewId);
            });

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    assertEquals("Review not found", exception.getReason());
  }

  @Test
  void updateReview_ShouldUpdateAndReturnDto_WhenUserIsAuthor() {
    UpdateReviewRequest request = new UpdateReviewRequest();
    request.setRating(4);
    request.setComment("Updated comment");

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class))).thenReturn(review);

    ReviewResponseDto result = reviewService.updateReview(reviewId, authorId, request);

    assertNotNull(result);
    assertEquals(4, result.getRating());
    assertEquals("Updated comment", result.getComment());
    verify(reviewRepository, times(1)).save(any(Review.class));
  }

  @Test
  void updateReview_ShouldThrowForbidden_WhenUserIsNotAuthor() {
    UUID otherUserId = UUID.randomUUID();
    UpdateReviewRequest request = new UpdateReviewRequest();
    request.setRating(4);

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              reviewService.updateReview(reviewId, otherUserId, request);
            });

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("Access denied: You can only update your own reviews", exception.getReason());
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void deleteReview_ShouldDelete_WhenUserAdminAndReviewExists() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    doNothing().when(reviewRepository).deleteById(reviewId);

    assertDoesNotThrow(() -> reviewService.deleteReview(reviewId, true));

    verify(reviewRepository, times(1)).deleteById(reviewId);
  }

  @Test
  void deleteReview_ShouldThrowForbidden_WhenUserNotAdmin() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              reviewService.deleteReview(reviewId, false);
            });

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("Access denied: Only admin can delete reviews", exception.getReason());
    verify(reviewRepository, never()).deleteById(any());
  }
}
