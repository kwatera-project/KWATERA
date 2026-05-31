package io.github.kwatera_project.kwatera.ai_pricing_service.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.ai_pricing_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.exception.PropertyNotFoundException;
import io.github.kwatera_project.kwatera.ai_pricing_service.exception.UnitNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PropertyClientTest {

  @Mock private RestTemplate restTemplate;

  @InjectMocks private PropertyClient propertyClient;

  private static final String PROPERTY_SERVICE_URL = "http://property-service";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(propertyClient, "propertyServiceUrl", PROPERTY_SERVICE_URL);
  }

  @Test
  void shouldReturnUnit() {
    // given
    UUID unitId = UUID.randomUUID();
    UnitDto unitDto = new UnitDto();

    when(restTemplate.getForEntity(PROPERTY_SERVICE_URL + "/units/" + unitId, UnitDto.class))
        .thenReturn(ResponseEntity.ok(unitDto));

    // when
    UnitDto result = propertyClient.getUnit(unitId);

    // then
    assertThat(result).isEqualTo(unitDto);
  }

  @Test
  void shouldThrowUnitNotFoundExceptionWhenUnitDoesNotExist() {
    // given
    UUID unitId = UUID.randomUUID();

    when(restTemplate.getForEntity(PROPERTY_SERVICE_URL + "/units/" + unitId, UnitDto.class))
        .thenThrow(HttpClientErrorException.NotFound.class);

    // when & then
    assertThatThrownBy(() -> propertyClient.getUnit(unitId))
        .isInstanceOf(UnitNotFoundException.class);
  }

  @Test
  void shouldReturnProperty() {
    // given
    UUID propertyId = UUID.randomUUID();
    PropertyDto propertyDto = new PropertyDto();

    when(restTemplate.getForEntity(PROPERTY_SERVICE_URL + "/" + propertyId, PropertyDto.class))
        .thenReturn(ResponseEntity.ok(propertyDto));

    // when
    PropertyDto result = propertyClient.getProperty(propertyId);

    // then
    assertThat(result).isEqualTo(propertyDto);
  }

  @Test
  void shouldThrowPropertyNotFoundExceptionWhenPropertyDoesNotExist() {
    // given
    UUID propertyId = UUID.randomUUID();

    when(restTemplate.getForEntity(PROPERTY_SERVICE_URL + "/" + propertyId, PropertyDto.class))
        .thenThrow(HttpClientErrorException.NotFound.class);

    // when & then
    assertThatThrownBy(() -> propertyClient.getProperty(propertyId))
        .isInstanceOf(PropertyNotFoundException.class);
  }
}
