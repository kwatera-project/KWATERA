package io.github.kwatera_project.kwatera.review_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.review_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewTranslationResponse;
import io.github.kwatera_project.kwatera.review_service.service.JwtService;
import io.github.kwatera_project.kwatera.review_service.service.ReviewTranslationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewTranslationController.class)
@Import(SecurityConfig.class)
class ReviewTranslationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewTranslationService translationService;
  @MockitoBean private JwtService jwtService;

  @Test
  void translatesReviewForRequestedLanguage() throws Exception {
    UUID reviewId = UUID.randomUUID();
    when(translationService.translate(reviewId, "pl"))
        .thenReturn(new ReviewTranslationResponse("Świetny pokój.", "pl", "EN", false));

    mockMvc
        .perform(
            post("/api/reviews/{reviewId}/translation", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("targetLanguage", "pl"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.translatedText").value("Świetny pokój."))
        .andExpect(jsonPath("$.targetLanguage").value("pl"))
        .andExpect(jsonPath("$.sourceLanguage").value("EN"))
        .andExpect(jsonPath("$.alreadyInTargetLanguage").value(false));

    verify(translationService).translate(reviewId, "pl");
  }

  @Test
  void rejectsUnsupportedTargetLanguage() throws Exception {
    UUID reviewId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/reviews/{reviewId}/translation", reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("targetLanguage", "de"))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(translationService);
  }
}
