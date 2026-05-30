package io.github.kwatera_project.kwatera.billing_service.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.billing_service.dto.OcrResponseDto;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrClient {

  private final RestTemplate restTemplate;

  @Value("${services.ocr.url}")
  private String ocrServiceUrl;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RestTemplate is a Spring-managed bean injected by the container.")
  public OcrClient(@Qualifier("plainRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public OcrResponseDto readMeter(MultipartFile file) throws IOException {
    ByteArrayResource resource =
        new ByteArrayResource(file.getBytes()) {
          @Override
          public String getFilename() {
            return file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "meter-image.jpg";
          }
        };

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<OcrResponseDto> response =
          restTemplate.postForEntity(
              ocrServiceUrl + "/ocr/read-meter", request, OcrResponseDto.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("OCR service returned status: " + response.getStatusCode());
      }

      if (response.getBody() == null) {
        throw new RuntimeException("OCR service returned empty response");
      }
      return response.getBody();

    } catch (RestClientException e) {
      throw new RuntimeException("Failed to call OCR service", e);
    }
  }
}
