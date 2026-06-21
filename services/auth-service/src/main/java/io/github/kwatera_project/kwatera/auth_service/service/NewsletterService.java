package io.github.kwatera_project.kwatera.auth_service.service;

import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NewsletterService {

  private final ChatClient chatClient;
  private final UserRepository userRepository;
  private final PropertyRepository propertyRepository;
  private final EmailNotificationService emailNotificationService;

  public NewsletterService(
      ChatClient.Builder chatClientBuilder,
      UserRepository userRepository,
      PropertyRepository propertyRepository,
      EmailNotificationService emailNotificationService) {
    this.chatClient = chatClientBuilder.build();
    this.userRepository = userRepository;
    this.propertyRepository = propertyRepository;
    this.emailNotificationService = emailNotificationService;
  }

  @Async
  public CompletableFuture<Void> sendPersonalizedNewsletterAsync(String email) {
    try {
      User user = userRepository.findByEmail(email).orElse(null);
      String firstName = (user != null && user.getFirstName() != null) ? user.getFirstName() : "Traveler";
      String preference = analyzePreference(user);
      List<Object[]> recommendations = fetchRecommendations(preference);

      StringBuilder propertiesText = new StringBuilder();
      for (Object[] prop : recommendations) {
        propertiesText.append("Title: ").append(prop[1]).append("\n");
        propertiesText.append("Location: ").append(prop[2]).append(", ").append(prop[3]).append("\n");
        propertiesText.append("Price: ").append(prop[4]).append(" PLN / night\n");
        propertiesText.append("Description: ").append(prop[6]).append("\n");
        propertiesText.append("Image URL: ").append(prop[5]).append("\n\n");
      }

      String systemPrompt =
          "You are a friendly travel advisor at KWATERA. "
              + "Write a short personalized greeting acknowledging the user's travel preference. "
              + "Format the 3 recommended properties nicely into a clean HTML structure suitable for an email. "
              + "Do not wrap your response in markdown code blocks like ```html. Return only the raw HTML code.";

      String userPrompt =
          "User name: " + firstName + "\n"
              + "Preference: " + preference + "\n"
              + "Recommended Properties:\n" + propertiesText.toString();

      String htmlBody = chatClient.prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();

      emailNotificationService.sendPersonalizedNewsletter(
          email,
          "Your Personalized KWATERA Recommendations",
          htmlBody
      );
    } catch (Exception e) {
      log.error("Failed to generate personalized email for " + email, e);
      try {
        emailNotificationService.sendWeeklyNewsletterEmail(email);
      } catch (Exception ex) {
        log.error("Failed to send fallback newsletter email to " + email, ex);
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  private String analyzePreference(User user) {
    if (user == null) {
      return "NEW_USER";
    }
    List<Object[]> details = userRepository.findPropertyDetailsByUserId(user.getId());
    if (details == null || details.isEmpty()) {
      return "NEW_USER";
    }
    int mountains = 0;
    int sea = 0;
    int city = 0;

    for (Object[] row : details) {
      String c = row[0] != null ? row[0].toString().toLowerCase() : "";
      String a = row[1] != null ? row[1].toString().toLowerCase() : "";

      if (c.contains("szczyrk") || c.contains("kościelisko") || c.contains("wetlina") || c.contains("karpacz")
          || a.contains("fireplace") || a.contains("sauna") || a.contains("hot tub")) {
        mountains++;
      } else if (c.contains("gdańsk") || c.contains("sopot") || c.contains("mikołajki")
          || a.contains("kayaks") || a.contains("beach") || a.contains("lake")) {
        sea++;
      } else if (c.contains("kraków") || c.contains("wrocław") || c.contains("warszawa")
          || a.contains("elevator") || a.contains("gym") || a.contains("air conditioning")) {
        city++;
      } else {
        city++;
      }
    }

    if (mountains >= sea && mountains >= city) {
      return "MOUNTAINS";
    } else if (sea >= mountains && sea >= city) {
      return "SEA";
    } else {
      return "CITY";
    }
  }

  private List<Object[]> fetchRecommendations(String preference) {
    List<Object[]> list;
    if ("MOUNTAINS".equals(preference)) {
      list = propertyRepository.findTop3PropertiesByCities(
          List.of("szczyrk", "kościelisko", "wetlina", "karpacz")
      );
    } else if ("SEA".equals(preference)) {
      list = propertyRepository.findTop3PropertiesByCities(
          List.of("gdańsk", "sopot", "mikołajki")
      );
    } else if ("CITY".equals(preference)) {
      list = propertyRepository.findTop3PropertiesByCities(
          List.of("kraków", "wrocław", "warszawa")
      );
    } else {
      list = propertyRepository.findTop3DefaultProperties();
    }

    if (list == null || list.isEmpty()) {
      list = propertyRepository.findTop3DefaultProperties();
    }
    return list;
  }
}
