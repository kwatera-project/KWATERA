package io.github.kwatera_project.kwatera.review_service.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import io.github.kwatera_project.kwatera.review_service.service.JwtService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:review-controller-it;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerIntegrationTest {

  private static final String TOKEN = "test-token";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ReviewRepository reviewRepository;

  @MockitoBean private JwtService jwtService;
  @MockitoBean private ReservationClient reservationClient;
  @MockitoBean private PropertyClient propertyClient;

  private UUID guestId;
  private UUID reservationId;
  private UUID unitId;

  @BeforeEach
  void setUp() {
    reviewRepository.deleteAll();

    guestId = UUID.randomUUID();
    reservationId = UUID.randomUUID();
    unitId = UUID.randomUUID();
  }

  @AfterEach
  void cleanUp() {
    reviewRepository.deleteAll();
  }

  @Test
  void shouldCreateReviewAndSaveCorrectData() throws Exception {
    authenticateGuest();
    stubReservation(guestId, "COMPLETED");

    submitReview()
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorId").value(guestId.toString()))
        .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
        .andExpect(jsonPath("$.unitId").value(unitId.toString()))
        .andExpect(jsonPath("$.rating").value(5));

    List<Review> reviews = reviewRepository.findAll();
    assertEquals(1, reviews.size());

    Review saved = reviews.getFirst();

    assertAll(
        () -> assertEquals(guestId, saved.getAuthorId()),
        () -> assertEquals(reservationId, saved.getReservationId()),
        () -> assertEquals(unitId, saved.getUnitId()),
        () -> assertEquals(5, saved.getRating()),
        () -> assertEquals("Great stay!", saved.getComment()),
        () -> assertNotNull(saved.getCreatedAt()));
  }

  @Test
  void shouldRejectAnotherGuestsReservation() throws Exception {
    authenticateGuest();
    stubReservation(UUID.randomUUID(), "COMPLETED");

    submitReview().andExpect(status().isForbidden());

    assertEquals(0L, reviewRepository.count());
  }

  @Test
  void shouldRejectReservationThatIsNotCompleted() throws Exception {
    authenticateGuest();
    stubReservation(guestId, "CONFIRMED");

    submitReview().andExpect(status().isConflict());

    assertEquals(0L, reviewRepository.count());
  }

  @Test
  void shouldRejectSecondReviewForSameReservation() throws Exception {
    authenticateGuest();
    stubReservation(guestId, "COMPLETED");

    submitReview().andExpect(status().isCreated());

    submitReview().andExpect(status().isConflict());

    assertEquals(1L, reviewRepository.count());
  }

  @Test
  void shouldReturnSubmittedReviewThroughPublicUnitEndpoint() throws Exception {
    authenticateGuest();
    stubReservation(guestId, "COMPLETED");

    submitReview().andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/reviews/units/{unitId}", unitId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviews.length()").value(1))
        .andExpect(jsonPath("$.reviews[0].unitId").value(unitId.toString()))
        .andExpect(jsonPath("$.reviews[0].rating").value(5))
        .andExpect(jsonPath("$.reviews[0].comment").value("Great stay!"))
        .andExpect(jsonPath("$.reviews[0].authorId").doesNotExist())
        .andExpect(jsonPath("$.reviews[0].reservationId").doesNotExist())
        .andExpect(jsonPath("$.reviewCount").value(1))
        .andExpect(jsonPath("$.averageRating").value(5.0));
  }

  @Test
  void shouldReturnPropertyReviewsWithPaginationAndSummary() throws Exception {
    UUID propertyId = UUID.randomUUID();
    UUID secondUnitId = UUID.randomUUID();
    UUID unrelatedUnitId = UUID.randomUUID();

    saveReview(unitId, 5);
    saveReview(secondUnitId, 3);
    saveReview(secondUnitId, 1);

    saveReview(unrelatedUnitId, 5);

    when(propertyClient.getUnitIdsForProperty(propertyId))
        .thenReturn(List.of(unitId, secondUnitId));

    mockMvc
        .perform(
            get("/api/reviews/properties/{propertyId}", propertyId)
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviews.length()").value(1))
        .andExpect(
            jsonPath("$.reviews[0].unitId")
                .value(
                    org.hamcrest.Matchers.isIn(
                        List.of(unitId.toString(), secondUnitId.toString()))))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.reviewCount").value(3))
        .andExpect(jsonPath("$.averageRating").value(3.0));
  }

  private void authenticateGuest() {
    when(jwtService.isValid(TOKEN)).thenReturn(true);
    when(jwtService.extractUsername(TOKEN)).thenReturn("test-guest");
    when(jwtService.extractUserId(TOKEN)).thenReturn(guestId.toString());
    when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("ROLE_GUEST"));
  }

  private void stubReservation(UUID reservationGuestId, String status) {
    LocalDate endDate = LocalDate.now(ZoneId.of("Europe/Warsaw")).minusDays(7);

    when(reservationClient.getReservation(reservationId))
        .thenReturn(
            new ReservationDetailsDto(reservationId, reservationGuestId, unitId, status, endDate));
  }

  private ResultActions submitReview() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);
    request.setComment("Great stay!");

    return mockMvc.perform(
        post("/api/reviews")
            .header("Authorization", "Bearer " + TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }

  private void saveReview(UUID reviewUnitId, int rating) {
    Review review = new Review();
    review.setReservationId(UUID.randomUUID());
    review.setAuthorId(guestId);
    review.setUnitId(reviewUnitId);
    review.setRating(rating);
    review.setComment("Test review");

    reviewRepository.saveAndFlush(review);
  }
}
