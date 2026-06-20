package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.auth_service.dto.SubscribeRequest;
import io.github.kwatera_project.kwatera.auth_service.model.NewsletterSubscriber;
import io.github.kwatera_project.kwatera.auth_service.repository.NewsletterSubscriberRepository;
import io.github.kwatera_project.kwatera.auth_service.service.EmailNotificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = NewsletterController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    })
class NewsletterControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NewsletterSubscriberRepository subscriberRepository;

  @MockitoBean private EmailNotificationService emailNotificationService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldSubscribeNewEmail() throws Exception {
    SubscribeRequest request = new SubscribeRequest();
    request.setEmail("new@example.com");

    when(subscriberRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Please check your email to confirm subscription."));

    verify(subscriberRepository).findByEmail("new@example.com");
    verify(subscriberRepository).save(any(NewsletterSubscriber.class));
    verify(emailNotificationService)
        .sendNewsletterVerificationEmail(eq("new@example.com"), anyString());
  }

  @Test
  void shouldResendVerificationWhenPending() throws Exception {
    SubscribeRequest request = new SubscribeRequest();
    request.setEmail("pending@example.com");

    NewsletterSubscriber subscriber = new NewsletterSubscriber();
    subscriber.setEmail("pending@example.com");
    subscriber.setStatus("PENDING");
    subscriber.setToken("old-token");

    when(subscriberRepository.findByEmail("pending@example.com"))
        .thenReturn(Optional.of(subscriber));

    mockMvc
        .perform(
            post("/api/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Please check your email to confirm subscription."));

    verify(subscriberRepository).findByEmail("pending@example.com");
    verify(subscriberRepository).save(subscriber);
    verify(emailNotificationService)
        .sendNewsletterVerificationEmail(eq("pending@example.com"), anyString());
  }

  @Test
  void shouldDoNothingWhenAlreadyConfirmed() throws Exception {
    SubscribeRequest request = new SubscribeRequest();
    request.setEmail("confirmed@example.com");

    NewsletterSubscriber subscriber = new NewsletterSubscriber();
    subscriber.setEmail("confirmed@example.com");
    subscriber.setStatus("CONFIRMED");
    subscriber.setToken("some-token");

    when(subscriberRepository.findByEmail("confirmed@example.com"))
        .thenReturn(Optional.of(subscriber));

    mockMvc
        .perform(
            post("/api/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Please check your email to confirm subscription."));

    verify(subscriberRepository).findByEmail("confirmed@example.com");
    verify(subscriberRepository, never()).save(any(NewsletterSubscriber.class));
    verify(emailNotificationService, never())
        .sendNewsletterVerificationEmail(anyString(), anyString());
  }

  @Test
  void shouldConfirmSubscription() throws Exception {
    NewsletterSubscriber subscriber = new NewsletterSubscriber();
    subscriber.setEmail("confirm@example.com");
    subscriber.setStatus("PENDING");
    subscriber.setToken("valid-token");

    when(subscriberRepository.findByToken("valid-token")).thenReturn(Optional.of(subscriber));

    mockMvc
        .perform(get("/api/newsletter/confirm?token=valid-token"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "http://localhost:5173/?newsletterConfirmed=true"));

    verify(subscriberRepository).findByToken("valid-token");
    verify(subscriberRepository).save(subscriber);
    verify(emailNotificationService).sendNewsletterWelcomeEmail("confirm@example.com");
  }

  @Test
  void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
    SubscribeRequest request = new SubscribeRequest();
    request.setEmail("invalid-email");

    mockMvc
        .perform(
            post("/api/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(subscriberRepository, never()).findByEmail(anyString());
    verify(subscriberRepository, never()).save(any(NewsletterSubscriber.class));
    verify(emailNotificationService, never())
        .sendNewsletterVerificationEmail(anyString(), anyString());
  }
}
