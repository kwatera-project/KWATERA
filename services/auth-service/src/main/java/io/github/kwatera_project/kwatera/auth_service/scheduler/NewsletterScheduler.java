package io.github.kwatera_project.kwatera.auth_service.scheduler;

import io.github.kwatera_project.kwatera.auth_service.model.NewsletterSubscriber;
import io.github.kwatera_project.kwatera.auth_service.repository.NewsletterSubscriberRepository;
import io.github.kwatera_project.kwatera.auth_service.service.NewsletterService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsletterScheduler {

  private final NewsletterSubscriberRepository subscriberRepository;
  private final NewsletterService newsletterService;

  @Scheduled(cron = "0 0 18 * * FRI")
  public void sendWeeklyPersonalizedNewsletters() {
    List<NewsletterSubscriber> activeSubscribers = subscriberRepository.findByStatus("CONFIRMED");
    for (NewsletterSubscriber subscriber : activeSubscribers) {
      newsletterService.sendPersonalizedNewsletterAsync(subscriber.getEmail());
    }
  }
}
