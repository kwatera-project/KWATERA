package io.github.kwatera_project.kwatera.review_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewService reviewService;

  @MockitoBean private JwtService jwtService;

  private UUID userId;
  private UUID reviewId;
  private UUID reservationId;
  private UsernamePasswordAuthenticationToken guestAuth;
  private UsernamePasswordAuthenticationToken adminAuth;
  private UsernamePasswordAuthenticationToken unauthUser;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    reviewId = UUID.randomUUID();
    reservationId = UUID.randomUUID();

    guestAuth =
        new UsernamePasswordAuthenticationToken(
            "testUser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    guestAuth.setDetails(userId.toString());

    adminAuth =
        new UsernamePasswordAuthenticationToken(
            "adminUser", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    adminAuth.setDetails(UUID.randomUUID().toString());

    unauthUser =
        new UsernamePasswordAuthenticationToken(
            "badUser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    unauthUser.setDetails("invalid-uuid");
  }

  @Test
  void createReview_ShouldReturnCreated_WhenGuestAndValidRequest() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);
    request.setComment("Amazing place!");

    Review savedReview = new Review();
    savedReview.setId(reviewId);
    savedReview.setAuthorId(userId);
    savedReview.setReservationId(reservationId);
    savedReview.setRating(5);
    savedReview.setComment("Amazing place!");
    savedReview.setCreatedAt(Instant.now());

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
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void createReview_ShouldReturnUnauthorized_WhenTokenDetailsInvalid() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(reservationId);
    request.setRating(5);

    mockMvc
        .perform(
            post("/api/reviews")
                .with(authentication(unauthUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getMyReviews_ShouldReturnList_WhenGuest() throws Exception {
    ReviewResponseDto responseDto =
        ReviewResponseDto.builder()
            .id(reviewId)
            .authorId(userId)
            .rating(5)
            .comment("Nice")
            .createdAt(Instant.now())
            .build();

    when(reviewService.getReviewsByAuthor(userId)).thenReturn(List.of(responseDto));

    mockMvc
        .perform(get("/api/reviews/my").with(authentication(guestAuth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
  }

  @Test
  void getReviewDetails_ShouldReturnDto() throws Exception {
    ReviewResponseDto responseDto =
        ReviewResponseDto.builder()
            .id(reviewId)
            .authorId(userId)
            .rating(4)
            .comment("Good")
            .createdAt(Instant.now())
            .build();

    when(reviewService.getReviewDetails(reviewId)).thenReturn(responseDto);

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

    ReviewResponseDto responseDto =
        ReviewResponseDto.builder()
            .id(reviewId)
            .authorId(userId)
            .rating(4)
            .comment("Updated")
            .createdAt(Instant.now())
            .build();

    when(reviewService.updateReview(eq(reviewId), eq(userId), any(UpdateReviewRequest.class)))
        .thenReturn(responseDto);

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
    doNothing().when(reviewService).deleteReview(reviewId, true);

    mockMvc
        .perform(delete("/api/reviews/{reviewId}", reviewId).with(authentication(adminAuth)))
        .andExpect(status().isNoContent());
  }

  @Test
  void getReviewDetailsInternal_ShouldReturnDto_WhenValidToken() throws Exception {
    ReviewResponseDto responseDto = ReviewResponseDto.builder().id(reviewId).rating(5).build();

    when(reviewService.getReviewDetails(reviewId)).thenReturn(responseDto);

    mockMvc
        .perform(
            get("/api/reviews/internal/{reviewId}", reviewId)
                .header("X-Internal-Token", "kwatera-internal-secret-token"))
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
  }
}
