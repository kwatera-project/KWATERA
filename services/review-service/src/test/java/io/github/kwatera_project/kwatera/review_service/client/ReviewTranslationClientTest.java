package io.github.kwatera_project.kwatera.review_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ReviewTranslationClientTest {

  private static final String URL = "http://ai-service/internal/ai/translate";

  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final ReviewTranslationClient client =
      new ReviewTranslationClient(restTemplate, "test-token");

  @Test
  void sendsReviewAndTargetLanguageToAiService() {
    server
        .expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Internal-Token", "test-token"))
        .andExpect(
            content()
                .json(
                    """
                    {"text":"Great room.","sourceLanguage":"auto","targetLanguage":"pl"}
                    """))
        .andRespond(
            withSuccess(
                """
                {"translatedText":"Świetny pokój.","sourceLanguage":"auto","targetLanguage":"pl","model":"translategemma:4b"}
                """,
                MediaType.APPLICATION_JSON));

    assertEquals("Świetny pokój.", client.translate("Great room.", "pl"));
    server.verify();
  }

  @Test
  void rejectsMissingInternalTokenBeforeCallingAiService() {
    var clientWithoutToken = new ReviewTranslationClient(restTemplate, " ");

    assertThrows(
        RestClientException.class,
        () -> clientWithoutToken.translate("Great room.", "pl"));
    server.verify();
  }
}
