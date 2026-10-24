package io.github.kwatera_project.kwatera.review_service.integration;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import io.github.kwatera_project.kwatera.review_service.service.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ReviewRepository reviewRepository;

  @MockitoBean private JwtService jwtService;

  @BeforeEach
  void setUp() {
    reviewRepository.deleteAll();

    when(jwtService.isValid("valid-test-token")).thenReturn(true);
    when(jwtService.extractUsername("valid-test-token")).thenReturn("guest-user");
    when(jwtService.extractUserId("valid-test-token"))
        .thenReturn("123e4567-e89b-12d3-a456-426614174000");
    when(jwtService.extractRoles("valid-test-token")).thenReturn(List.of("ROLE_GUEST"));
  }

  @Test
  @WithMockUser(
      username = "guest-user",
      authorities = {"ROLE_GUEST"})
  void shouldCreateReviewSuccessfullyWhenRequestIsValid() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(UUID.randomUUID());
    request.setRating(5);
    request.setComment("Amazing stay, highly recommended!");

    mockMvc
        .perform(
            post("/api/reviews")
                .header("Authorization", "Bearer valid-test-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rating", is(5)))
        .andExpect(jsonPath("$.comment", is("Amazing stay, highly recommended!")));
  }

  @Test
  @WithMockUser(
      username = "guest-user",
      authorities = {"ROLE_GUEST"})
  void shouldRejectReviewWhenRatingIsInvalid() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(UUID.randomUUID());
    request.setRating(6);
    request.setComment("Too high rating.");

    mockMvc
        .perform(
            post("/api/reviews")
                .header("Authorization", "Bearer valid-test-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(
      username = "guest-user",
      authorities = {"ROLE_GUEST"})
  void shouldRejectReviewWhenReservationIdIsNull() throws Exception {
    CreateReviewRequest request = new CreateReviewRequest();
    request.setReservationId(null);
    request.setRating(4);
    request.setComment("Missing reservation ID.");

    mockMvc
        .perform(
            post("/api/reviews")
                .header("Authorization", "Bearer valid-test-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
