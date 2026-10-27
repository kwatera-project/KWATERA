package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.dto.UpdateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReservationClient reservationClient;

  @InjectMocks private ReviewService reviewService;

  private UUID reviewId;
  private UUID authorId;
  private UUID reservationId;
  private UUID unitId;
  private Review review;

  @BeforeEach
  void setUp() {
    reviewId = UUID.randomUUID();
    authorId = UUID.randomUUID();
    reservationId = UUID.randomUUID();
    unitId = UUID.randomUUID();

    review = new Review();
    review.setId(reviewId);
    review.setAuthorId(authorId);
    review.setReservationId(reservationId);
    review.setUnitId(unitId);
    review.setRating(5);
    review.setComment("Great place!");

    Instant timestamp = Instant.parse("2026-10-27T12:00:00Z");
    review.setCreatedAt(timestamp);
    review.setUpdatedAt(timestamp);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void createReview_ShouldSave_OnLastDayOrAfterStay(int endDateOffset) {
    LocalDate endDate = TODAY.plusDays(endDateOffset);
    stubReservation(authorId, "COMPLETED", endDate);

    when(reviewRepository.saveAndFlush(any(Review.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Review created = createReviewOnFixedDate();

    assertAll(
        () -> assertEquals(authorId, created.getAuthorId()),
        () -> assertEquals(reservationId, created.getReservationId()),
        () -> assertEquals(unitId, created.getUnitId()),
        () -> assertEquals(5, created.getRating()),
        () -> assertEquals("Great place!", created.getComment()));

    verify(reservationClient).getReservation(reservationId);
    verify(reviewRepository).existsByReservationId(reservationId);
    verify(reviewRepository).saveAndFlush(same(created));
  }

  @Test
  void createReview_ShouldReject_WhenReservationBelongsToAnotherGuest() {
    stubReservation(UUID.randomUUID(), "COMPLETED", TODAY);
    CreateReviewRequest request = validRequest();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.createReview(authorId, request));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verifyNoInteractions(reviewRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "CONFIRMED", "CANCELLED", "BLOCKED"})
  void createReview_ShouldReject_WhenReservationIsNotCompleted(String status) {
    stubReservation(authorId, status, TODAY.minusDays(1));
    CreateReviewRequest request = validRequest();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.createReview(authorId, request));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void createReview_ShouldReject_BeforeLastDayOfStay() {
    stubReservation(authorId, "COMPLETED", TODAY.plusDays(1));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, this::createReviewOnFixedDate);

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void createReview_ShouldReject_WhenEndDateIsMissing() {
    stubReservation(authorId, "COMPLETED", null);
    CreateReviewRequest request = validRequest();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.createReview(authorId, request));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void createReview_ShouldReject_WhenReviewAlreadyExists() {
    stubReservation(authorId, "COMPLETED", TODAY);
    when(reviewRepository.existsByReservationId(reservationId)).thenReturn(true);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, this::createReviewOnFixedDate);

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verify(reviewRepository).existsByReservationId(reservationId);
    verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @ParameterizedTest
  @EnumSource(
      value = HttpStatus.class,
      names = {"NOT_FOUND", "BAD_GATEWAY", "SERVICE_UNAVAILABLE"})
  void createReview_ShouldNotSave_WhenReservationClientFails(HttpStatus status) {
    ResponseStatusException clientException =
        new ResponseStatusException(status, "Reservation lookup failed");

    when(reservationClient.getReservation(reservationId)).thenThrow(clientException);
    CreateReviewRequest request = validRequest();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.createReview(authorId, request));

    assertSame(clientException, exception);
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void getReviewsByAuthor_ShouldReturnListOfDtos() {
    when(reviewRepository.findReviewsByAuthorId(authorId)).thenReturn(List.of(review));

    List<ReviewResponseDto> result = reviewService.getReviewsByAuthor(authorId);

    assertEquals(1, result.size());

    ReviewResponseDto dto = result.getFirst();

    assertAll(
        () -> assertEquals(reviewId, dto.getId()),
        () -> assertEquals(authorId, dto.getAuthorId()),
        () -> assertEquals(reservationId, dto.getReservationId()),
        () -> assertEquals(5, dto.getRating()),
        () -> assertEquals("Great place!", dto.getComment()),
        () -> assertEquals(review.getCreatedAt(), dto.getCreatedAt()),
        () -> assertEquals(review.getUpdatedAt(), dto.getUpdatedAt()));
  }

  @Test
  void getReviewsByAuthor_ShouldReturnEmptyList_WhenAuthorHasNoReviews() {
    when(reviewRepository.findReviewsByAuthorId(authorId)).thenReturn(List.of());

    List<ReviewResponseDto> result = reviewService.getReviewsByAuthor(authorId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getReviewDetails_ShouldReturnDto_WhenReviewExists() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    ReviewResponseDto result = reviewService.getReviewDetails(reviewId);

    assertAll(
        () -> assertEquals(reviewId, result.getId()),
        () -> assertEquals(authorId, result.getAuthorId()),
        () -> assertEquals(reservationId, result.getReservationId()),
        () -> assertEquals(5, result.getRating()),
        () -> assertEquals("Great place!", result.getComment()),
        () -> assertEquals(review.getCreatedAt(), result.getCreatedAt()),
        () -> assertEquals(review.getUpdatedAt(), result.getUpdatedAt()));
  }

  @Test
  void getReviewDetails_ShouldThrowNotFound_WhenReviewDoesNotExist() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> reviewService.getReviewDetails(reviewId));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void updateReview_ShouldUpdate_WhenUserIsAuthor() {
    UpdateReviewRequest request = new UpdateReviewRequest();
    request.setRating(4);
    request.setComment("Updated comment");

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ReviewResponseDto result = reviewService.updateReview(reviewId, authorId, request);

    assertEquals(4, result.getRating());
    assertEquals("Updated comment", result.getComment());
    verify(reviewRepository).save(review);
  }

  @Test
  void updateReview_ShouldThrowForbidden_WhenUserIsNotAuthor() {
    UpdateReviewRequest request = new UpdateReviewRequest();
    request.setRating(4);
    UUID otherUserId = UUID.randomUUID();

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> reviewService.updateReview(reviewId, otherUserId, request));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void updateReview_ShouldThrowNotFound_WhenReviewDoesNotExist() {
    UpdateReviewRequest request = new UpdateReviewRequest();

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> reviewService.updateReview(reviewId, authorId, request));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void deleteReview_ShouldDelete_WhenUserIsAdminAndReviewExists() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    reviewService.deleteReview(reviewId, true);

    verify(reviewRepository).deleteById(reviewId);
  }

  @Test
  void deleteReview_ShouldThrowForbidden_WhenUserIsNotAdmin() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.deleteReview(reviewId, false));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void deleteReview_ShouldThrowNotFound_WhenReviewDoesNotExist() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewService.deleteReview(reviewId, true));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(reviewRepository, never()).deleteById(any(UUID.class));
  }

  private CreateReviewRequest validRequest() {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);
    request.setComment("Great place!");
    return request;
  }

  private void stubReservation(UUID guestId, String status, LocalDate endDate) {
    ReservationDetailsDto reservation =
        new ReservationDetailsDto(reservationId, guestId, unitId, status, endDate);

    when(reservationClient.getReservation(reservationId)).thenReturn(reservation);
  }

  private Review createReviewOnFixedDate() {
    try (MockedStatic<LocalDate> dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
      dates.when(() -> LocalDate.now(ZONE)).thenReturn(TODAY);

      return reviewService.createReview(authorId, validRequest());
    }
  }
}
