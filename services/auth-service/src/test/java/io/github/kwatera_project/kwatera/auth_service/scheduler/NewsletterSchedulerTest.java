package io.github.kwatera_project.kwatera.auth_service.scheduler;

import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.model.NewsletterSubscriber;
import io.github.kwatera_project.kwatera.auth_service.repository.NewsletterSubscriberRepository;
import io.github.kwatera_project.kwatera.auth_service.service.NewsletterService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsletterSchedulerTest {

  @Mock private NewsletterSubscriberRepository subscriberRepository;
  @Mock private NewsletterService newsletterService;

  private NewsletterScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new NewsletterScheduler(subscriberRepository, newsletterService);
  }

  @Test
  void shouldSendNewsletterToAllConfirmedSubscribers() {
    NewsletterSubscriber s1 = buildSubscriber("alice@example.com");
    NewsletterSubscriber s2 = buildSubscriber("bob@example.com");
    when(subscriberRepository.findByStatus("CONFIRMED")).thenReturn(List.of(s1, s2));

    scheduler.sendWeeklyPersonalizedNewsletters();

    verify(newsletterService).sendPersonalizedNewsletterAsync("alice@example.com");
    verify(newsletterService).sendPersonalizedNewsletterAsync("bob@example.com");
  }

  @Test
  void shouldDoNothing_whenNoConfirmedSubscribers() {
    when(subscriberRepository.findByStatus("CONFIRMED")).thenReturn(List.of());

    scheduler.sendWeeklyPersonalizedNewsletters();

    verifyNoInteractions(newsletterService);
  }

  @Test
  void shouldQueryOnlyConfirmedStatus() {
    when(subscriberRepository.findByStatus("CONFIRMED")).thenReturn(List.of());

    scheduler.sendWeeklyPersonalizedNewsletters();

    verify(subscriberRepository).findByStatus("CONFIRMED");
    verifyNoMoreInteractions(subscriberRepository);
  }

  private NewsletterSubscriber buildSubscriber(String email) {
    NewsletterSubscriber subscriber = new NewsletterSubscriber();
    subscriber.setEmail(email);
    return subscriber;
  }
}
