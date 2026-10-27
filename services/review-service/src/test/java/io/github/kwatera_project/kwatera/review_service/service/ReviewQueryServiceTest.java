package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.dto.PublicReviewDto;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewPageDto;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private PropertyClient propertyClient;

  @InjectMocks private ReviewQueryService reviewQueryService;

  private UUID unitId;
  private UUID propertyId;
  private Review review;

  @BeforeEach
  void setUp() {
    unitId = UUID.randomUUID();
    propertyId = UUID.randomUUID();
    review = createReview(unitId, 5);
  }

  @Test
  void shouldReturnUnitReviewAndSummaryForAllReviews() {
    List<UUID> unitIds = List.of(unitId);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 1), 8));

    when(reviewRepository.averageRatingForUnits(unitIds)).thenReturn(4.125);

    ReviewPageDto result = reviewQueryService.getForUnit(unitId, 0, 1);

    assertAll(
        () -> assertEquals(1, result.reviews().size()),
        () -> assertEquals(0, result.page()),
        () -> assertEquals(1, result.size()),
        () -> assertEquals(8, result.totalPages()),
        () -> assertEquals(8L, result.reviewCount()),
        () -> assertEquals(new BigDecimal("4.13"), result.averageRating()));

    PublicReviewDto dto = result.reviews().getFirst();

    assertAll(
        () -> assertEquals(review.getId(), dto.id()),
        () -> assertEquals(unitId, dto.unitId()),
        () -> assertEquals(5, dto.rating()),
        () -> assertEquals("Test review", dto.comment()),
        () -> assertEquals(review.getCreatedAt(), dto.createdAt()),
        () -> assertEquals(review.getUpdatedAt(), dto.updatedAt()));

    verify(propertyClient).requireUnit(unitId);
    verify(reviewRepository).averageRatingForUnits(unitIds);
  }

  @Test
  void shouldPassPaginationAndSortToRepository() {
    List<UUID> unitIds = List.of(unitId);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(2, 5), 11));

    ReviewPageDto result = reviewQueryService.getForUnit(unitId, 2, 5);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

    verify(reviewRepository).findByUnitIdIn(eq(unitIds), captor.capture());

    Pageable pageable = captor.getValue();

    assertAll(
        () -> assertEquals(2, pageable.getPageNumber()),
        () -> assertEquals(5, pageable.getPageSize()),
        () ->
            assertEquals(
                List.of(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
                pageable.getSort().toList()),
        () -> assertEquals(2, result.page()),
        () -> assertEquals(5, result.size()),
        () -> assertEquals(3, result.totalPages()));
  }

  @Test
  void shouldReturnEmptyResultWhenUnitHasNoReviews() {
    List<UUID> unitIds = List.of(unitId);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(Page.empty(PageRequest.of(0, 10)));

    when(reviewRepository.averageRatingForUnits(unitIds)).thenReturn(null);

    ReviewPageDto result = reviewQueryService.getForUnit(unitId, 0, 10);

    assertEmptyResult(result);
    verify(propertyClient).requireUnit(unitId);
  }

  @Test
  void shouldKeepSummaryWhenRequestedPageIsEmpty() {
    List<UUID> unitIds = List.of(unitId);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(new PageImpl<Review>(List.of(), PageRequest.of(5, 10), 3));

    when(reviewRepository.averageRatingForUnits(unitIds)).thenReturn(4.0);

    ReviewPageDto result = reviewQueryService.getForUnit(unitId, 5, 10);

    assertAll(
        () -> assertTrue(result.reviews().isEmpty()),
        () -> assertEquals(5, result.page()),
        () -> assertEquals(3L, result.reviewCount()),
        () -> assertEquals(1, result.totalPages()),
        () -> assertEquals(new BigDecimal("4.00"), result.averageRating()));
  }

  @Test
  void shouldReturnReviewsForAllPropertyUnits() {
    UUID secondUnitId = UUID.randomUUID();
    List<UUID> unitIds = List.of(unitId, secondUnitId);
    Review secondReview = createReview(secondUnitId, 3);

    when(propertyClient.getUnitIdsForProperty(propertyId)).thenReturn(unitIds);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(review, secondReview), PageRequest.of(0, 10), 2));

    when(reviewRepository.averageRatingForUnits(unitIds)).thenReturn(4.0);

    ReviewPageDto result = reviewQueryService.getForProperty(propertyId, 0, 10);

    assertAll(
        () -> assertEquals(2L, result.reviewCount()),
        () ->
            assertEquals(
                List.of(unitId, secondUnitId),
                result.reviews().stream().map(PublicReviewDto::unitId).toList()),
        () -> assertEquals(new BigDecimal("4.00"), result.averageRating()));

    verify(propertyClient).getUnitIdsForProperty(propertyId);
    verify(reviewRepository).averageRatingForUnits(unitIds);
  }

  @Test
  void shouldReturnEmptyResultWhenPropertyHasNoUnits() {
    when(propertyClient.getUnitIdsForProperty(propertyId)).thenReturn(List.of());

    ReviewPageDto result = reviewQueryService.getForProperty(propertyId, 0, 10);

    assertEmptyResult(result);
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void shouldReturnEmptyResultWhenPropertyUnitsHaveNoReviews() {
    List<UUID> unitIds = List.of(unitId);

    when(propertyClient.getUnitIdsForProperty(propertyId)).thenReturn(unitIds);

    when(reviewRepository.findByUnitIdIn(eq(unitIds), any(Pageable.class)))
        .thenReturn(Page.empty(PageRequest.of(0, 10)));

    when(reviewRepository.averageRatingForUnits(unitIds)).thenReturn(null);

    ReviewPageDto result = reviewQueryService.getForProperty(propertyId, 0, 10);

    assertEmptyResult(result);
    verify(reviewRepository).averageRatingForUnits(unitIds);
  }

  @ParameterizedTest
  @CsvSource({"-1, 10", "0, 0", "0, -1", "0, 101"})
  void shouldRejectInvalidUnitPagination(int page, int size) {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> reviewQueryService.getForUnit(unitId, page, size));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verifyNoInteractions(propertyClient, reviewRepository);
  }

  @Test
  void shouldValidatePaginationForPropertyToo() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> reviewQueryService.getForProperty(propertyId, -1, 10));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verifyNoInteractions(propertyClient, reviewRepository);
  }

  @ParameterizedTest
  @CsvSource({"404, Accommodation not found", "503, Property service unavailable"})
  void shouldPropagateUnitLookupError(int status, String message) {
    ResponseStatusException expected =
        new ResponseStatusException(HttpStatus.valueOf(status), message);

    doThrow(expected).when(propertyClient).requireUnit(unitId);

    ResponseStatusException actual =
        assertThrows(
            ResponseStatusException.class, () -> reviewQueryService.getForUnit(unitId, 0, 10));

    assertSame(expected, actual);
    verifyNoInteractions(reviewRepository);
  }

  @Test
  void shouldRejectMissingProperty() {
    ResponseStatusException expected =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found");

    when(propertyClient.getUnitIdsForProperty(propertyId)).thenThrow(expected);

    ResponseStatusException actual =
        assertThrows(
            ResponseStatusException.class,
            () -> reviewQueryService.getForProperty(propertyId, 0, 10));

    assertSame(expected, actual);
    verifyNoInteractions(reviewRepository);
  }

  private void assertEmptyResult(ReviewPageDto result) {
    assertAll(
        () -> assertTrue(result.reviews().isEmpty()),
        () -> assertEquals(0, result.page()),
        () -> assertEquals(10, result.size()),
        () -> assertEquals(0, result.totalPages()),
        () -> assertEquals(0L, result.reviewCount()),
        () -> assertNull(result.averageRating()));
  }

  private Review createReview(UUID reviewUnitId, int rating) {
    Review result = new Review();
    result.setId(UUID.randomUUID());
    result.setUnitId(reviewUnitId);
    result.setRating(rating);
    result.setComment("Test review");
    result.setCreatedAt(Instant.parse("2026-10-27T10:00:00Z"));
    result.setUpdatedAt(Instant.parse("2026-10-27T11:00:00Z"));
    return result;
  }
}
