package io.github.kwatera_project.kwatera.auth_service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class NewsletterService {

  private final ChatClient chatClient;
  private final UserRepository userRepository;
  private final PropertyRepository propertyRepository;
  private final EmailNotificationService emailNotificationService;
  private final TemplateEngine templateEngine;
  private final String frontendBaseUrl;
  private final String publicGatewayUrl;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NewsletterService(
      ChatClient.Builder chatClientBuilder,
      UserRepository userRepository,
      PropertyRepository propertyRepository,
      EmailNotificationService emailNotificationService,
      TemplateEngine templateEngine,
      @Value("${kwatera.urls.frontend-base}") String frontendBaseUrl,
      @Value("${kwatera.urls.public-gateway}") String publicGatewayUrl) {
    this.chatClient = chatClientBuilder.build();
    this.userRepository = userRepository;
    this.propertyRepository = propertyRepository;
    this.emailNotificationService = emailNotificationService;
    this.templateEngine = templateEngine;
    this.frontendBaseUrl = frontendBaseUrl;
    this.publicGatewayUrl = publicGatewayUrl;
  }

  @Async
  public CompletableFuture<Void> sendPersonalizedNewsletterAsync(String email) {
    try {
      User user = userRepository.findByEmail(email).orElse(null);
      String firstName =
          (user != null && user.getFirstName() != null) ? user.getFirstName() : "Traveler";
      String preference = analyzePreference(user);
      List<Object[]> recommendations = fetchRecommendations(preference);

      String personalizedGreeting = generateGreeting(firstName, preference);
      String propertiesRationale = generateRationale(preference, recommendations);

      List<Map<String, Object>> featuredItems = buildFeaturedItems(recommendations);

      Context context = new Context();
      context.setVariable("subject", "Your Personalized KWATERA Recommendations");
      context.setVariable("greeting", firstName);
      context.setVariable("personalizedGreeting", personalizedGreeting);
      context.setVariable("propertiesRationale", propertiesRationale);
      context.setVariable("featuredItems", featuredItems);
      context.setVariable(
          "unsubscribeLink", publicGatewayUrl + "/api/newsletter/unsubscribe?email=" + email);

      String htmlBody = templateEngine.process("personalized-newsletter-template", context);

      emailNotificationService.sendPersonalizedNewsletter(
          email, "Your Personalized KWATERA Recommendations", htmlBody);
    } catch (Exception e) {
      log.error("Failed to generate personalized email for {}", email, e);
      try {
        emailNotificationService.sendWeeklyNewsletterEmail(email);
      } catch (Exception ex) {
        log.error("Failed to send fallback newsletter email to {}", email, ex);
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  private String generateGreeting(String firstName, String preference) {
    String prompt =
        switch (preference) {
          case "MOUNTAINS" ->
              "Write one short, warm sentence (max 20 words) for a newsletter greeting "
                  + "for "
                  + firstName
                  + " who loves mountain getaways. Do not include any HTML.";
          case "SEA" ->
              "Write one short, warm sentence (max 20 words) for a newsletter greeting "
                  + "for "
                  + firstName
                  + " who loves seaside and lake destinations. Do not include any HTML.";
          case "CITY" ->
              "Write one short, warm sentence (max 20 words) for a newsletter greeting "
                  + "for "
                  + firstName
                  + " who enjoys city breaks. Do not include any HTML.";
          default ->
              "Write one short, warm sentence (max 20 words) for a newsletter greeting "
                  + "for "
                  + firstName
                  + " who is just starting to explore travel. Do not include any HTML.";
        };
    try {
      return chatClient.prompt().user(prompt).call().content();
    } catch (Exception e) {
      log.warn("LLM greeting generation failed, using default", e);
      return "Here are this week's handpicked properties just for you.";
    }
  }

  private String generateRationale(String preference, List<Object[]> recommendations) {
    StringBuilder titles = new StringBuilder();
    for (Object[] prop : recommendations) {
      if (prop[1] != null) {
        titles.append("- ").append(prop[1]).append("\n");
      }
    }
    String preferenceLabel =
        switch (preference) {
          case "MOUNTAINS" -> "mountain escapes";
          case "SEA" -> "seaside and lake destinations";
          case "CITY" -> "city breaks";
          default -> "travel";
        };
    String prompt =
        "Write exactly one sentence (max 25 words) explaining why these properties were "
            + "recommended for someone who loves "
            + preferenceLabel
            + ". "
            + "Properties:\n"
            + titles
            + "Be specific, friendly, and do not include any HTML.";
    try {
      return chatClient.prompt().user(prompt).call().content();
    } catch (Exception e) {
      log.warn("LLM rationale generation failed, using default", e);
      return "These properties were selected based on your travel preferences.";
    }
  }

  private List<Map<String, Object>> buildFeaturedItems(List<Object[]> recommendations) {
    List<Map<String, Object>> items = new ArrayList<>();
    for (Object[] prop : recommendations) {
      Map<String, Object> item = new HashMap<>();
      item.put("title", prop[1]);
      item.put("description", prop[6]);
      item.put("imageUrl", prop[5]);
      item.put("link", frontendBaseUrl + "/property/" + prop[0]);
      BigDecimal price = null;
      if (prop[4] != null) {
        try {
          price = new BigDecimal(prop[4].toString());
        } catch (NumberFormatException ignored) {
          // fall through to default
        }
      }
      item.put("pricePerNight", price != null ? price : new BigDecimal("250"));
      items.add(item);
    }
    return items;
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

      if (c.contains("szczyrk")
          || c.contains("kościelisko")
          || c.contains("wetlina")
          || c.contains("karpacz")
          || a.contains("fireplace")
          || a.contains("sauna")
          || a.contains("hot tub")) {
        mountains++;
      } else if (c.contains("gdańsk")
          || c.contains("sopot")
          || c.contains("mikołajki")
          || a.contains("kayaks")
          || a.contains("beach")
          || a.contains("lake")) {
        sea++;
      } else {
        city++;
      }
    }

    if (mountains > sea && mountains > city) {
      return "MOUNTAINS";
    } else if (sea > mountains && sea > city) {
      return "SEA";
    } else if (city > mountains && city > sea) {
      return "CITY";
    } else {
      return "CITY";
    }
  }

  private List<Object[]> fetchRecommendations(String preference) {
    List<Object[]> list;
    if ("MOUNTAINS".equals(preference)) {
      list =
          propertyRepository.findTop3PropertiesByCities(
              List.of("szczyrk", "kościelisko", "wetlina", "karpacz"));
    } else if ("SEA".equals(preference)) {
      list = propertyRepository.findTop3PropertiesByCities(List.of("gdańsk", "sopot", "mikołajki"));
    } else if ("CITY".equals(preference)) {
      list =
          propertyRepository.findTop3PropertiesByCities(List.of("kraków", "wrocław", "warszawa"));
    } else {
      list = propertyRepository.findTop3DefaultProperties();
    }

    if (list == null || list.isEmpty()) {
      list = propertyRepository.findTop3DefaultProperties();
    }
    return list;
  }
}
