package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.dto.UpdateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

  private static final String REVIEW_NOT_FOUND = "Review not found";

  private final ReviewRepository reviewRepository;
  private final ReservationClient reservationClient;

  @Autowired
  public ReviewService(ReviewRepository reviewRepository, ReservationClient reservationClient) {
    this.reviewRepository = reviewRepository;
    this.reservationClient = reservationClient;
  }

  @Transactional
  public Review createReview(UUID authorId, CreateReviewRequest request) {
    ReservationDetailsDto reservation =
        reservationClient.getReservation(request.getReservationId());

    if (!reservation.userId().equals(authorId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only review your own reservation");
    }

    if (!"COMPLETED".equals(reservation.status())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only completed reservations can be reviewed");
    }
    if (reservation.endDate() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Reservation service returned no end date");
    }

    LocalDate today = LocalDate.now(ZoneId.of("Europe/Warsaw"));

    if (today.isBefore(reservation.endDate())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Reviews can be submitted from the last day of the stay");
    }

    if (reviewRepository.existsByReservationId(reservation.id())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A review already exists for this reservation");
    }

    Review review = new Review();
    review.setAuthorId(authorId);
    review.setReservationId(reservation.id());
    review.setUnitId(reservation.unitId());
    review.setRating(request.getRating());
    review.setComment(request.getComment());

    return reviewRepository.saveAndFlush(review);
  }

  public List<ReviewResponseDto> getReviewsByAuthor(UUID userId) {
    return reviewRepository.findReviewsByAuthorId(userId).stream()
        .map(
            review ->
                ReviewResponseDto.builder()
                    .id(review.getId())
                    .reservationId(review.getReservationId())
                    .authorId(review.getAuthorId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build())
        .toList();
  }

  public ReviewResponseDto getReviewDetails(UUID reviewId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, REVIEW_NOT_FOUND));

    return ReviewResponseDto.builder()
        .id(review.getId())
        .reservationId(review.getReservationId())
        .authorId(review.getAuthorId())
        .rating(review.getRating())
        .comment(review.getComment())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .build();
  }

  @Transactional
  public ReviewResponseDto updateReview(UUID reviewId, UUID userId, UpdateReviewRequest request) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, REVIEW_NOT_FOUND));

    boolean isAuthor = review.getAuthorId().equals(userId);
    if (!isAuthor) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: You can only update your own reviews");
    }

    if (request.getRating() != null) {
      review.setRating(request.getRating());
    }
    if (request.getComment() != null) {
      review.setComment(request.getComment());
    }

    Review updatedReview = reviewRepository.save(review);

    return ReviewResponseDto.builder()
        .id(updatedReview.getId())
        .reservationId(updatedReview.getReservationId())
        .authorId(updatedReview.getAuthorId())
        .rating(updatedReview.getRating())
        .comment(updatedReview.getComment())
        .createdAt(updatedReview.getCreatedAt())
        .updatedAt(updatedReview.getUpdatedAt())
        .build();
  }

  @Transactional
  public void deleteReview(UUID reviewId, boolean isAdmin) {

    if (!isAdmin) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: Only admin can delete reviews");
    }

    reviewRepository
        .findById(reviewId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, REVIEW_NOT_FOUND));

    reviewRepository.deleteById(reviewId);
  }
}
