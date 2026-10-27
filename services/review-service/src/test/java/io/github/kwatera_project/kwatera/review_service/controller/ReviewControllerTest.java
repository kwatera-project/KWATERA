package io.github.kwatera_project.kwatera.review_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.review_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.dto.UpdateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.service.JwtService;
import io.github.kwatera_project.kwatera.review_service.service.ReviewService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "kwatera.security.internal-token=test-internal-token")
class ReviewControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewService reviewService;
  @MockitoBean private JwtService jwtService;

  private UUID userId;
  private UUID reviewId;
  private UUID reservationId;
  private UUID unitId;

  private UsernamePasswordAuthenticationToken guestAuth;
  private UsernamePasswordAuthenticationToken adminAuth;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    reviewId = UUID.randomUUID();
    reservationId = UUID.randomUUID();
    unitId = UUID.randomUUID();

    guestAuth = auth("ROLE_GUEST", userId.toString());
    adminAuth = auth("ROLE_ADMIN", UUID.randomUUID().toString());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5})
  void createReview_ShouldReturnCreated_WhenGuestAndValidRequest(int rating) throws Exception {

    CreateReviewRequest request = validRequest();
    request.setRating(rating);

    Review savedReview = new Review();
    savedReview.setId(reviewId);
    savedReview.setAuthorId(userId);
    savedReview.setReservationId(reservationId);
    savedReview.setUnitId(unitId);
    savedReview.setRating(rating);
    savedReview.setComment(request.getComment());
    savedReview.setCreatedAt(Instant.parse("2026-10-27T12:00:00Z"));

    when(reviewService.createReview(eq(userId), any(CreateReviewRequest.class)))
        .thenReturn(savedReview);

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.authorId").value(userId.toString()))
        .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
        .andExpect(jsonPath("$.unitId").value(unitId.toString()))
        .andExpect(jsonPath("$.rating").value(rating))
        .andExpect(jsonPath("$.comment").value("Amazing place!"));

    verify(reviewService)
        .createReview(
            eq(userId),
            argThat(
                submitted ->
                    reservationId.equals(submitted.getReservationId())
                        && Integer.valueOf(rating).equals(submitted.getRating())
                        && "Amazing place!".equals(submitted.getComment())));
  }

  @Test
  void createReview_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {

    mockMvc
        .perform(
            post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(reviewService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_OWNER", "ROLE_ADMIN"})
  void createReview_ShouldReturnForbidden_WhenUserIsNotGuest(String role) throws Exception {

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(auth(role, userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isForbidden());

    verifyNoInteractions(reviewService);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "invalid-uuid"})
  void createReview_ShouldReturnUnauthorized_WhenUserIdIsInvalid(String details) throws Exception {

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(auth("ROLE_GUEST", details)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(reviewService);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, 6})
  void createReview_ShouldReturnBadRequest_WhenRatingIsInvalid(Integer rating) throws Exception {

    CreateReviewRequest request = validRequest();
    request.setRating(rating);

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  void createReview_ShouldReturnBadRequest_WhenReservationIdIsMissing() throws Exception {

    CreateReviewRequest request = validRequest();
    request.setReservationId(null);

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  void createReview_ShouldReturnBadRequest_WhenJsonIsMalformed() throws Exception {

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @Test
  void createReview_ShouldReturnBadRequest_WhenReservationIdIsMalformed() throws Exception {

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reservationId": "not-a-uuid",
                      "rating": 5,
                      "comment": "Amazing place!"
                    }
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reviewService);
  }

  @ParameterizedTest
  @EnumSource(
      value = HttpStatus.class,
      names = {"NOT_FOUND", "BAD_GATEWAY", "SERVICE_UNAVAILABLE"})
  void createReview_ShouldReturnStatusReportedByService(HttpStatus expectedStatus)
      throws Exception {

    when(reviewService.createReview(eq(userId), any(CreateReviewRequest.class)))
        .thenThrow(new ResponseStatusException(expectedStatus, "Review submission rejected"));

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().is(expectedStatus.value()));

    verify(reviewService).createReview(eq(userId), any(CreateReviewRequest.class));
  }

  @Test
  void getMyReviews_ShouldReturnList_WhenGuest() throws Exception {
    ReviewResponseDto response = reviewResponse(5, "Nice");

    when(reviewService.getReviewsByAuthor(userId)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/reviews/my").with(authentication(guestAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(reviewId.toString()));

    verify(reviewService).getReviewsByAuthor(userId);
  }

  @Test
  void getReviewDetails_ShouldReturnDto() throws Exception {
    when(reviewService.getReviewDetails(reviewId)).thenReturn(reviewResponse(4, "Good"));

    mockMvc
        .perform(get("/api/reviews/{reviewId}", reviewId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.rating").value(4));
  }

  @Test
  void updateReview_ShouldReturnUpdatedDto() throws Exception {
    UpdateReviewRequest request = new UpdateReviewRequest();
    request.setRating(4);
    request.setComment("Updated");

    when(reviewService.updateReview(eq(reviewId), eq(userId), any(UpdateReviewRequest.class)))
        .thenReturn(reviewResponse(4, "Updated"));

    mockMvc
        .perform(
            put("/api/reviews/{reviewId}", reviewId)
                .with(authentication(guestAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comment").value("Updated"));
  }

  @Test
  void deleteReview_ShouldReturnNoContent_WhenAdmin() throws Exception {
    mockMvc
        .perform(delete("/api/reviews/{reviewId}", reviewId).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());

    verify(reviewService).deleteReview(reviewId, true);
  }

  @Test
  void getReviewDetailsInternal_ShouldReturnDto_WhenValidToken() throws Exception {

    when(reviewService.getReviewDetails(reviewId)).thenReturn(reviewResponse(5, "Nice"));

    mockMvc
        .perform(
            get("/api/reviews/internal/{reviewId}", reviewId)
                .header("X-Internal-Token", "test-internal-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reviewId.toString()));
  }

  @Test
  void getReviewDetailsInternal_ShouldReturnForbidden_WhenInvalidToken() throws Exception {

    mockMvc
        .perform(
            get("/api/reviews/internal/{reviewId}", reviewId)
                .header("X-Internal-Token", "wrong-token"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(reviewService);
  }

  @Test
  void getReviewDetailsInternal_ShouldReturnForbidden_WhenTokenIsMissing() throws Exception {

    mockMvc
        .perform(get("/api/reviews/internal/{reviewId}", reviewId))
        .andExpect(status().isForbidden());

    verifyNoInteractions(reviewService);
  }

  private CreateReviewRequest validRequest() {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);
    request.setComment("Amazing place!");
    return request;
  }

  private ReviewResponseDto reviewResponse(int rating, String comment) {
    return ReviewResponseDto.builder()
        .id(reviewId)
        .authorId(userId)
        .reservationId(reservationId)
        .rating(rating)
        .comment(comment)
        .createdAt(Instant.parse("2026-10-27T12:00:00Z"))
        .build();
  }

  private UsernamePasswordAuthenticationToken auth(String role, String userIdDetails) {

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "test-user", null, List.of(new SimpleGrantedAuthority(role)));

    authentication.setDetails(userIdDetails);
    return authentication;
  }
}
