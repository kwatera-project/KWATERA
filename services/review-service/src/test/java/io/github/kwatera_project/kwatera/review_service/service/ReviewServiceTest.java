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

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.ZoneOffset;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReservationClient reservationClient;

    @Mock private Root<Review> criteriaRoot;
    @Mock private CriteriaQuery<?> criteriaQuery;
    @Mock private CriteriaBuilder criteriaBuilder;
    @Mock private Path<Instant> createdAtPath;
    @Mock private Path<Instant> updatedAtPath;
    @Mock private Predicate predicate;
    @Mock private Order order;

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

  @Test
  void getReviewsForAdmin_ShouldReturnAllReviews_WhenNoFiltersProvided() {
    when(reviewRepository.findAll(any(Specification.class))).thenReturn(List.of(review));

    List<ReviewResponseDto> result = reviewService.getReviewsForAdmin(null, null, null);

    assertEquals(1, result.size());
    ReviewResponseDto dto = result.getFirst();
    assertEquals(reviewId, dto.getId());
    verify(reviewRepository).findAll(any(Specification.class));
  }

  @Test
  void getReviewsForAdmin_ShouldFilterByStartDateAndCreatedAt() {
    LocalDate startDate = LocalDate.of(2026, 10, 1);
    when(reviewRepository.findAll(any(Specification.class))).thenReturn(List.of(review));

    List<ReviewResponseDto> result = reviewService.getReviewsForAdmin(startDate, null, "createdAt");

    assertEquals(1, result.size());
    verify(reviewRepository).findAll(any(Specification.class));
  }

  @Test
  void getReviewsForAdmin_ShouldFilterByEndDateAndUpdatedAt() {
    LocalDate endDate = LocalDate.of(2026, 10, 31);
    when(reviewRepository.findAll(any(Specification.class))).thenReturn(List.of(review));

    List<ReviewResponseDto> result = reviewService.getReviewsForAdmin(null, endDate, "updatedAt");

    assertEquals(1, result.size());
    verify(reviewRepository).findAll(any(Specification.class));
  }


    @Test
    void getReviewsForAdmin_ShouldBuildSpecificationWithStartAndEndDate() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Specification<Review> spec =
                captureSpecification(startDate, endDate, "createdAt");

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");
        when(criteriaBuilder.greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.lessThanOrEqualTo(
                createdAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result =
                spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        assertEquals(predicate, result);

        verify(criteriaRoot, times(3)).get("createdAt");

        verify(criteriaBuilder).greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant());

        verify(criteriaBuilder).lessThanOrEqualTo(
                createdAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant());

        verify(criteriaBuilder).desc(createdAtPath);
        verify(criteriaQuery).orderBy(order);
        verify(criteriaBuilder).and(any(Predicate[].class));
    }

    @Test
    void getReviewsForAdmin_ShouldUseUpdatedAt_WhenDateTypeIsUpdatedAt() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Specification<Review> spec =
                captureSpecification(startDate, endDate, "updatedAt");

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");
        doReturn(updatedAtPath).when(criteriaRoot).get("updatedAt");

        when(criteriaBuilder.greaterThanOrEqualTo(
                updatedAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.lessThanOrEqualTo(
                updatedAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result =
                spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        assertEquals(predicate, result);

        verify(criteriaRoot, times(2)).get("updatedAt");
        verify(criteriaRoot).get("createdAt");

        verify(criteriaBuilder).greaterThanOrEqualTo(
                updatedAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant());

        verify(criteriaBuilder).lessThanOrEqualTo(
                updatedAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant());

        // Sortowanie zawsze po createdAt, nawet gdy filtrujemy updatedAt.
        verify(criteriaBuilder).desc(createdAtPath);
        verify(criteriaQuery).orderBy(order);
    }

    @Test
    void getReviewsForAdmin_ShouldAddOnlyStartDatePredicate_WhenEndDateIsNull() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);

        Specification<Review> spec =
                captureSpecification(startDate, null, "createdAt");

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");

        when(criteriaBuilder.greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant());

        verify(criteriaBuilder, never())
                .lessThanOrEqualTo(
                        ArgumentMatchers.<Expression<Instant>>any(),
                        ArgumentMatchers.<Instant>any());

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(1, captor.getValue().length);
        verify(criteriaQuery).orderBy(order);
    }

    @Test
    void getReviewsForAdmin_ShouldAddOnlyEndDatePredicate_WhenStartDateIsNull() {
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Specification<Review> spec =
                captureSpecification(null, endDate, "createdAt");

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");

        when(criteriaBuilder.lessThanOrEqualTo(
                createdAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        verify(criteriaBuilder, never())
                .greaterThanOrEqualTo(
                        ArgumentMatchers.<Expression<Instant>>any(),
                        ArgumentMatchers.<Instant>any());

        verify(criteriaBuilder).lessThanOrEqualTo(
                createdAtPath,
                endDate.atTime(LocalTime.MAX)
                        .atZone(ZoneOffset.UTC)
                        .toInstant());

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(1, captor.getValue().length);
        verify(criteriaQuery).orderBy(order);
    }

    @Test
    void getReviewsForAdmin_ShouldReturnEmptyPredicates_WhenBothDatesAreNull() {
        Specification<Review> spec =
                captureSpecification(null, null, null);

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");
        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        verify(criteriaBuilder, never())
                .lessThanOrEqualTo(
                        ArgumentMatchers.<Expression<Instant>>any(),
                        ArgumentMatchers.<Instant>any());

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(0, captor.getValue().length);

        verify(criteriaBuilder).desc(createdAtPath);
        verify(criteriaQuery).orderBy(order);
    }

    @Test
    void getReviewsForAdmin_ShouldUseCreatedAt_WhenDateTypeIsUnknown() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);

        Specification<Review> spec =
                captureSpecification(startDate, null, "unknown");

        doReturn(createdAtPath).when(criteriaRoot).get("createdAt");

        when(criteriaBuilder.greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(predicate);

        when(criteriaBuilder.desc(createdAtPath)).thenReturn(order);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(criteriaRoot, criteriaQuery, criteriaBuilder);

        verify(criteriaRoot, times(2)).get("createdAt");
        verify(criteriaRoot, never()).get("updatedAt");

        verify(criteriaBuilder).greaterThanOrEqualTo(
                createdAtPath,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    }


    private Specification<Review> captureSpecification(
            LocalDate startDate,
            LocalDate endDate,
            String dateType) {

        when(reviewRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(review));

        reviewService.getReviewsForAdmin(startDate, endDate, dateType);

        ArgumentCaptor<Specification<Review>> captor =
                ArgumentCaptor.forClass(Specification.class);

        verify(reviewRepository).findAll(captor.capture());

        return captor.getValue();
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
