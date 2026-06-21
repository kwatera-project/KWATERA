package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

  @Mock private ChatClient.Builder chatClientBuilder;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;
  @Mock private UserRepository userRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private TemplateEngine templateEngine;

  private NewsletterService newsletterService;

  private static final String FRONTEND_URL = "http://localhost:5173";
  private static final String GATEWAY_URL = "http://localhost:8090";

  @BeforeEach
  void setUp() {
    when(chatClientBuilder.build()).thenReturn(chatClient);
    newsletterService =
        new NewsletterService(
            chatClientBuilder,
            userRepository,
            propertyRepository,
            emailNotificationService,
            templateEngine,
            FRONTEND_URL,
            GATEWAY_URL);
  }

  // ---------------------------------------------------------------------------
  // sendPersonalizedNewsletterAsync — happy path for each preference
  // ---------------------------------------------------------------------------

  @Test
  void shouldSendPersonalizedEmail_forMountainPreference() throws Exception {
    User user = buildUser("Anna");
    when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Szczyrk", "fireplace"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("1", "Chata Górska", "200")));
    stubLlm("Góry czekają!");
    when(templateEngine.process(eq("personalized-newsletter-template"), any(Context.class)))
        .thenReturn("<html>personalized</html>");

    CompletableFuture<Void> result =
        newsletterService.sendPersonalizedNewsletterAsync("anna@example.com");

    result.get();
    verify(emailNotificationService)
        .sendPersonalizedNewsletter(
            eq("anna@example.com"),
            eq("Your Personalized KWATERA Recommendations"),
            eq("<html>personalized</html>"));
  }

  @Test
  void shouldSendPersonalizedEmail_forSeaPreference() throws Exception {
    User user = buildUser("Piotr");
    when(userRepository.findByEmail("piotr@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Sopot", "beach"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("2", "Apartament Morski", "300")));
    stubLlm("Morze czeka!");
    when(templateEngine.process(eq("personalized-newsletter-template"), any(Context.class)))
        .thenReturn("<html>sea</html>");

    newsletterService.sendPersonalizedNewsletterAsync("piotr@example.com").get();

    verify(emailNotificationService)
        .sendPersonalizedNewsletter(eq("piotr@example.com"), anyString(), eq("<html>sea</html>"));
  }

  @Test
  void shouldSendPersonalizedEmail_forCityPreference() throws Exception {
    User user = buildUser("Kasia");
    when(userRepository.findByEmail("kasia@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Kraków", "wifi"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("3", "Loft w centrum", "180")));
    stubLlm("Miasto na wyciągnięcie ręki!");
    when(templateEngine.process(eq("personalized-newsletter-template"), any(Context.class)))
        .thenReturn("<html>city</html>");

    newsletterService.sendPersonalizedNewsletterAsync("kasia@example.com").get();

    verify(emailNotificationService)
        .sendPersonalizedNewsletter(eq("kasia@example.com"), anyString(), eq("<html>city</html>"));
  }

  @Test
  void shouldSendPersonalizedEmail_forNewUserFallbackToDefault() throws Exception {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(propertyRepository.findTop3DefaultProperties())
        .thenReturn(properties(buildProperty("4", "Domek letniskowy", "150")));
    stubLlm("Witamy!");
    when(templateEngine.process(eq("personalized-newsletter-template"), any(Context.class)))
        .thenReturn("<html>new</html>");

    newsletterService.sendPersonalizedNewsletterAsync("new@example.com").get();

    verify(emailNotificationService)
        .sendPersonalizedNewsletter(eq("new@example.com"), anyString(), eq("<html>new</html>"));
    verify(propertyRepository).findTop3DefaultProperties();
  }

  @Test
  void shouldUseDefaultProperties_whenPreferenceResultIsEmpty() throws Exception {
    User user = buildUser("Marek");
    when(userRepository.findByEmail("marek@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Kraków", "wifi"));
    when(propertyRepository.findTop3PropertiesByCities(any())).thenReturn(new ArrayList<>());
    when(propertyRepository.findTop3DefaultProperties())
        .thenReturn(properties(buildProperty("5", "Dom nad jeziorem", "220")));
    stubLlm("Super oferty!");
    when(templateEngine.process(eq("personalized-newsletter-template"), any(Context.class)))
        .thenReturn("<html>fallback</html>");

    newsletterService.sendPersonalizedNewsletterAsync("marek@example.com").get();

    verify(propertyRepository).findTop3DefaultProperties();
    verify(emailNotificationService).sendPersonalizedNewsletter(anyString(), anyString(), anyString());
  }

  // ---------------------------------------------------------------------------
  // Thymeleaf context variables
  // ---------------------------------------------------------------------------

  @Test
  void shouldPassCorrectVariablesToTemplate() throws Exception {
    User user = buildUser("Ola");
    when(userRepository.findByEmail("ola@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Karpacz", "sauna"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("10", "Schronisko", "120")));
    stubLlm("Odpocznij w górach!");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("ola@example.com").get();

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("personalized-newsletter-template"), contextCaptor.capture());

    Context ctx = contextCaptor.getValue();
    assertThat(ctx.getVariable("greeting")).isEqualTo("Ola");
    assertThat(ctx.getVariable("personalizedGreeting")).isNotNull();
    assertThat(ctx.getVariable("propertiesRationale")).isNotNull();
    assertThat(ctx.getVariable("unsubscribeLink"))
        .isEqualTo(GATEWAY_URL + "/api/newsletter/unsubscribe?email=ola@example.com");
    @SuppressWarnings("unchecked")
    List<?> items = (List<?>) ctx.getVariable("featuredItems");
    assertThat(items).hasSize(1);
  }

  // ---------------------------------------------------------------------------
  // Fallback — LLM throws, default text used
  // ---------------------------------------------------------------------------

  @Test
  void shouldUseDefaultGreeting_whenLlmFails() throws Exception {
    User user = buildUser("Bartek");
    when(userRepository.findByEmail("bartek@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Gdańsk", "beach"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("6", "Apartament Morski", "250")));
    when(chatClient.prompt()).thenThrow(new RuntimeException("LLM unavailable"));
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("bartek@example.com").get();

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(anyString(), contextCaptor.capture());
    assertThat(contextCaptor.getValue().getVariable("personalizedGreeting"))
        .isEqualTo("Here are this week's handpicked properties just for you.");
    assertThat(contextCaptor.getValue().getVariable("propertiesRationale"))
        .isEqualTo("These properties were selected based on your travel preferences.");
  }

  // ---------------------------------------------------------------------------
  // Exception in outer block → sends fallback weekly newsletter
  // ---------------------------------------------------------------------------

  @Test
  void shouldSendFallbackWeeklyEmail_whenTemplateEngineFails() throws Exception {
    User user = buildUser("Tomek");
    when(userRepository.findByEmail("tomek@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Wrocław", "gym"));
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("7", "Studio Miejskie", "160")));
    stubLlm("Odkryj miasto!");
    when(templateEngine.process(anyString(), any(Context.class)))
        .thenThrow(new RuntimeException("Template error"));

    newsletterService.sendPersonalizedNewsletterAsync("tomek@example.com").get();

    verify(emailNotificationService).sendWeeklyNewsletterEmail("tomek@example.com");
    verify(emailNotificationService, never())
        .sendPersonalizedNewsletter(anyString(), anyString(), anyString());
  }

  // ---------------------------------------------------------------------------
  // analyzePreference — preference routing
  // ---------------------------------------------------------------------------

  @Test
  void shouldRouteToCityQuery_whenCityPreference() throws Exception {
    User user = buildUser("Ela");
    when(userRepository.findByEmail("ela@example.com")).thenReturn(Optional.of(user));
    // 2 city entries → CITY wins
    List<Object[]> details = new ArrayList<>();
    details.add(new Object[]{"Warszawa", "wifi", null, null, null, null, null});
    details.add(new Object[]{"Wrocław", "gym", null, null, null, null, null});
    when(userRepository.findPropertyDetailsByUserId(user.getId())).thenReturn(details);
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("8", "Loft", "200")));
    stubLlm("Witaj!");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("ela@example.com").get();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> citiesCaptor = ArgumentCaptor.forClass(List.class);
    verify(propertyRepository).findTop3PropertiesByCities(citiesCaptor.capture());
    assertThat(citiesCaptor.getValue()).contains("kraków", "wrocław", "warszawa");
  }

  @Test
  void shouldRouteToMountainQuery_whenMountainPreference() throws Exception {
    User user = buildUser("Janek");
    when(userRepository.findByEmail("janek@example.com")).thenReturn(Optional.of(user));
    List<Object[]> details = new ArrayList<>();
    details.add(new Object[]{"Karpacz", "sauna", null, null, null, null, null});
    details.add(new Object[]{"Szczyrk", "fireplace", null, null, null, null, null});
    when(userRepository.findPropertyDetailsByUserId(user.getId())).thenReturn(details);
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("9", "Chata", "180")));
    stubLlm("Góry!");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("janek@example.com").get();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> citiesCaptor = ArgumentCaptor.forClass(List.class);
    verify(propertyRepository).findTop3PropertiesByCities(citiesCaptor.capture());
    assertThat(citiesCaptor.getValue()).contains("szczyrk", "kościelisko", "wetlina", "karpacz");
  }

  @Test
  void shouldRouteToSeaQuery_whenSeaPreference() throws Exception {
    User user = buildUser("Magda");
    when(userRepository.findByEmail("magda@example.com")).thenReturn(Optional.of(user));
    List<Object[]> details = new ArrayList<>();
    details.add(new Object[]{"Sopot", "beach", null, null, null, null, null});
    details.add(new Object[]{"Gdańsk", "kayaks", null, null, null, null, null});
    when(userRepository.findPropertyDetailsByUserId(user.getId())).thenReturn(details);
    when(propertyRepository.findTop3PropertiesByCities(any()))
        .thenReturn(properties(buildProperty("11", "Apartament", "280")));
    stubLlm("Morze!");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("magda@example.com").get();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> citiesCaptor = ArgumentCaptor.forClass(List.class);
    verify(propertyRepository).findTop3PropertiesByCities(citiesCaptor.capture());
    assertThat(citiesCaptor.getValue()).contains("gdańsk", "sopot", "mikołajki");
  }

  // ---------------------------------------------------------------------------
  // buildFeaturedItems — null price falls back to 250
  // ---------------------------------------------------------------------------

  @Test
  void shouldUseDefaultPrice_whenPropertyPriceIsNull() throws Exception {
    User user = buildUser("Zosia");
    when(userRepository.findByEmail("zosia@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPropertyDetailsByUserId(user.getId()))
        .thenReturn(propertyDetails("Sopot", "kayaks"));
    List<Object[]> propsWithNullPrice = new ArrayList<>();
    propsWithNullPrice.add(new Object[]{"9", "Willa Morska", null, null, null, null, "Opis"});
    when(propertyRepository.findTop3PropertiesByCities(any())).thenReturn(propsWithNullPrice);
    stubLlm("Odpocznij!");
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");

    newsletterService.sendPersonalizedNewsletterAsync("zosia@example.com").get();

    ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(anyString(), captor.capture());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items =
        (List<Map<String, Object>>) captor.getValue().getVariable("featuredItems");
    assertThat(items).hasSize(1);
    assertThat(items.get(0).get("pricePerNight")).isEqualTo(new BigDecimal("250"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private User buildUser(String firstName) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setFirstName(firstName);
    return user;
  }

  /** [0]=id, [1]=title, [2]=city, [3]=address, [4]=price, [5]=imageUrl, [6]=description */
  private Object[] buildProperty(String id, String title, String price) {
    return new Object[]{id, title, "Kraków", "ul. Testowa 1", price, "http://img/" + id, "Opis"};
  }

  private List<Object[]> properties(Object[] prop) {
    List<Object[]> list = new ArrayList<>();
    list.add(prop);
    return list;
  }

  private List<Object[]> propertyDetails(String city, String amenity) {
    List<Object[]> list = new ArrayList<>();
    list.add(new Object[]{city, amenity, null, null, null, null, null});
    return list;
  }

  private void stubLlm(String response) {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn(response);
  }
}
