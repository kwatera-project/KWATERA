package io.github.kwatera_project.kwatera.ai_service.config;

import io.micrometer.observation.ObservationRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Configuration
public class OllamaConfiguration {
  private static final ScopedValue<Boolean> TRANSLATION = ScopedValue.newInstance();

  public static <T> T withoutPromptTruncation(Supplier<T> call) {
    return ScopedValue.where(TRANSLATION, true).call(call::get);
  }

  public static OllamaApi api(AiProperties properties, Duration readTimeout) {
    return api(properties, readTimeout, RestClient.builder());
  }

  private static OllamaApi api(
      AiProperties properties, Duration readTimeout, RestClient.Builder restClient) {
    var client = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(readTimeout);
    return OllamaApi.builder()
        .baseUrl(properties.baseUrl())
        .restClientBuilder(restClient.requestFactory(factory))
        .responseErrorHandler(new DefaultResponseErrorHandler())
        .build();
  }

  @Bean
  public OllamaChatModel ollamaChatModel(
      AiProperties properties,
      ObservationRegistry registry,
      @Qualifier("inferenceOllamaApi") OllamaApi api) {
    return OllamaChatModel.builder()
        .ollamaApi(api)
        .options(
            OllamaChatOptions.builder()
                .model(properties.summaryModel())
                .temperature(properties.temperature())
                .seed(246)
                .topK(40)
                .topP(0.9)
                .numPredict(properties.maxOutputTokens())
                .numCtx(properties.contextTokens())
                .build())
        .observationRegistry(registry)
        .modelManagementOptions(
            ModelManagementOptions.builder().pullModelStrategy(PullModelStrategy.NEVER).build())
        .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
        .build();
  }

  @Bean("inferenceOllamaApi")
  public OllamaApi inferenceOllamaApi(AiProperties properties, ObjectMapper mapper) {
    // Spring AI 2.0.2 does not write the top-level Ollama chat truncate option.
    var restClient =
        RestClient.builder()
            .requestInterceptor(
                (request, body, execution) -> {
                  if (!TRANSLATION.isBound()) return execution.execute(request, body);
                  var chat = mapper.readValue(body, ObjectNode.class);
                  chat.put("truncate", false);
                  byte[] payload = mapper.writeValueAsBytes(chat);
                  request.getHeaders().setContentLength(payload.length);
                  return execution.execute(request, payload);
                });
    return api(properties, properties.requestTimeout(), restClient);
  }

  @Bean("readinessOllamaApi")
  public OllamaApi readinessOllamaApi(AiProperties properties) {
    return api(properties, properties.readinessTimeout());
  }
}
