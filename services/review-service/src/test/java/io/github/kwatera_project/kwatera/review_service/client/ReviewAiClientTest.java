package io.github.kwatera_project.kwatera.review_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ReviewAiClientTest {

  private static final String URL = "http://ai-service/internal/ai/summarize";

  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final ReviewAiClient client = new ReviewAiClient(restTemplate, "test-token");

  @Test
  void shouldSendCommentsAndLanguageAndReturnSummary() {
    server
        .expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Internal-Token", "test-token"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
            {"reviews":["Czysty pokój.","Wieczorem było głośno."],"language":"pl"}
            """))
        .andRespond(
            withSuccess(
                """
            {"summary":"Czysty pokój, ale wieczorem było głośno.","pros":["Czystość"],"cons":["Hałas"],"model":"qwen"}
            """,
                MediaType.APPLICATION_JSON));

    var result = client.summarize(List.of("Czysty pokój.", "Wieczorem było głośno."), "pl");

    assertEquals("Czysty pokój, ale wieczorem było głośno.", result.summary());
    assertEquals(List.of("Czystość"), result.pros());
    assertEquals(List.of("Hałas"), result.cons());
    assertEquals("qwen", result.model());
    server.verify();
  }

  @Test
  void shouldRejectBlankSummary() {
    server
        .expect(requestTo(URL))
        .andRespond(
            withSuccess(
                """
            {"summary":" ","pros":[],"cons":[],"model":"qwen"}
            """,
                MediaType.APPLICATION_JSON));

    List<String> comments = List.of("Czysty pokój.");

    assertThrows(RestClientException.class, () -> client.summarize(comments, "pl"));
    server.verify();
  }

  @Test
  void shouldAcceptEmptyPoints() {
    server.expect(requestTo(URL)).andRespond(withSuccess(
        """
        {"summary":"Opis pobytu","pros":[],"cons":[],"model":"qwen"}
        """, MediaType.APPLICATION_JSON));

    var result = client.summarize(List.of("Opis pobytu"), "pl");

    assertEquals(List.of(), result.pros());
    assertEquals(List.of(), result.cons());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {
      "{\"pros\":[]}",
      "{\"pros\":null,\"cons\":[]}",
      "{\"pros\":[],\"cons\":[\" \"]}"
  })
  void shouldRejectInvalidPoints(String fields) {
    String json = "{\"summary\":\"Opis\",\"model\":\"qwen\"," + fields.substring(1);
    server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    List<String> comments = List.of("Opis pobytu");
    assertThrows(RestClientException.class, () -> client.summarize(comments, "pl"));
  }

  @Test
  void shouldReportUnavailableAiService() {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    List<String> comments = List.of("Czysty pokój.");

    assertThrows(RestClientException.class, () -> client.summarize(comments, "pl"));
    server.verify();
  }
}
