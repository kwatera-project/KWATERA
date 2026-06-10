package io.github.kwatera_project.kwatera.property_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.property_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.property_service.dto.*;
import io.github.kwatera_project.kwatera.property_service.model.*;
import io.github.kwatera_project.kwatera.property_service.repository.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class PropertyServiceTest {

  private PropertyRepository propertyRepository;
  private UnitRepository unitRepository;
  private PropertyService propertyService;
  private PropertyImageRepository propertyImageRepository;
  private UnitImageRepository unitImageRepository;
  private UnitSettlementItemRepository unitSettlementItemRepository;
  private io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient
      nbpExchangeRateClient;
  private ReservationClient reservationClient;
  private GeocodingService geocodingService;
  private MockedStatic<Files> mockedFiles;

  @BeforeEach
  void setUp() {
    propertyRepository = mock(PropertyRepository.class);
    unitRepository = mock(UnitRepository.class);
    propertyImageRepository = mock(PropertyImageRepository.class);
    unitImageRepository = mock(UnitImageRepository.class);
    unitSettlementItemRepository = mock(UnitSettlementItemRepository.class);
    nbpExchangeRateClient =
        mock(io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient.class);
    reservationClient = mock(ReservationClient.class);
    geocodingService = mock(GeocodingService.class);

    propertyService =
        new PropertyService(
            propertyRepository,
            unitRepository,
            propertyImageRepository,
            unitImageRepository,
            unitSettlementItemRepository,
            nbpExchangeRateClient,
            reservationClient,
            geocodingService);

    ReflectionTestUtils.setField(propertyService, "fileServerUrl", "http://localhost:8083");

    mockedFiles = mockStatic(Files.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
  }

  @Test
  void getAll_shouldReturnProperties() {
    Property property = new Property();
    property.setId(UUID.randomUUID());
    property.setTitle("Test");
    property.setCity("Warsaw");
    property.setDescription("Desc");

    when(propertyRepository.findByBoundingBoxAndAmenities(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of(property));

    var result = propertyService.getAll(null);

    assertEquals(1, result.size());
    assertEquals("Test", result.get(0).getTitle());
  }

  @Test
  void getByBoundingBox_shouldReturnFilteredProperties() {
    Property property = new Property();
    property.setId(UUID.randomUUID());
    property.setTitle("Test");
    property.setCity("Warsaw");
    property.setDescription("Desc");

    BigDecimal minLat = BigDecimal.valueOf(50);
    BigDecimal maxLat = BigDecimal.valueOf(53);
    BigDecimal minLng = BigDecimal.valueOf(19);
    BigDecimal maxLng = BigDecimal.valueOf(22);

    when(propertyRepository.findByBoundingBoxAndAmenities(
            eq(minLat), eq(maxLat), eq(minLng), eq(maxLng), any(), any()))
        .thenReturn(List.of(property));

    var result = propertyService.getByBoundingBox(minLat, maxLat, minLng, maxLng, null);

    assertEquals(1, result.size());
    assertEquals("Test", result.get(0).getTitle());
  }

  @Test
  void getById_shouldReturnProperty() {
    UUID id = UUID.randomUUID();

    Property property = new Property();
    property.setId(id);
    property.setTitle("Test");
    property.setCity("Warsaw");
    property.setDescription("Desc");

    when(propertyRepository.findById(id)).thenReturn(Optional.of(property));

    var result = propertyService.getById(id);

    assertEquals(id, result.getId());
    assertNull(result.getImageUrl());
  }

  @Test
  void getById_shouldThrowWhenNotFound() {
    UUID id = UUID.randomUUID();

    when(propertyRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> propertyService.getById(id));
  }

  @Test
  void getUnits_shouldReturnUnits() {
    UUID propertyId = UUID.randomUUID();

    when(propertyRepository.existsById(propertyId)).thenReturn(true);

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setName("Room");
    unit.setDescription("Desc");
    unit.setCapacity(2);
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit));

    var result = propertyService.getUnits(propertyId, "PLN");

    assertEquals(1, result.size());
    assertEquals("Room", result.get(0).getName());
  }

  @Test
  void getUnits_shouldThrowWhenPropertyNotExists() {
    UUID propertyId = UUID.randomUUID();

    when(propertyRepository.existsById(propertyId)).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> propertyService.getUnits(propertyId, "PLN"));
  }

  @Test
  void getUnitById_shouldReturnUnit() {
    UUID id = UUID.randomUUID();

    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setDescription("Desc");
    unit.setCapacity(2);
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

    var result = propertyService.getUnitById(id, "PLN");

    assertEquals(id, result.getId());
  }

  @Test
  void getUnitById_shouldThrowWhenNotFound() {
    UUID id = UUID.randomUUID();

    when(unitRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> propertyService.getUnitById(id, "PLN"));
  }

  @Test
  void getUnitIdsByOwnerId_shouldReturnEmptyWhenNoProperties() {
    UUID ownerId = UUID.randomUUID();
    when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of());

    var result = propertyService.getUnitIdsByOwnerId(ownerId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getUnitIdsByOwnerId_shouldReturnUnitIdsWhenPropertiesExist() {
    UUID ownerId = UUID.randomUUID();

    Property property = new Property();
    property.setId(UUID.randomUUID());

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());

    when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
    when(unitRepository.findByPropertyIdIn(List.of(property.getId()))).thenReturn(List.of(unit));

    var result = propertyService.getUnitIdsByOwnerId(ownerId);

    assertEquals(1, result.size());
    assertEquals(unit.getId(), result.get(0));
  }

  @Test
  void getPropertyImages_shouldReturnImages() {
    UUID propertyId = UUID.randomUUID();

    PropertyImage mockImage = new PropertyImage();
    mockImage.setId(UUID.randomUUID());
    mockImage.setUrl(
        "http://localhost:8083/properties/aaaa1111-1111-1111-1111-111111111111/4dc49eff-91e1-45db-8aab-dd61bd758fb5.jpg");
    mockImage.setIsMain(true);
    mockImage.setPropertyId(propertyId);

    List<PropertyImage> mockImages = List.of(mockImage);

    when(propertyImageRepository.findByPropertyId(propertyId)).thenReturn(mockImages);

    var result = propertyService.getPropertyImages(propertyId);

    assertEquals(1, result.size());
    assertEquals(
        "http://localhost:8083/properties/aaaa1111-1111-1111-1111-111111111111/4dc49eff-91e1-45db-8aab-dd61bd758fb5.jpg",
        result.get(0).getUrl());
  }

  @Test
  void getAllUnitIds_shouldReturnUnitIds() {
    Unit u1 = new Unit();
    u1.setId(UUID.randomUUID());

    Unit u2 = new Unit();
    u2.setId(UUID.randomUUID());

    when(unitRepository.findAll()).thenReturn(List.of(u1, u2));

    List<UUID> result = propertyService.getAllUnitIds();

    assertEquals(2, result.size());
    assertTrue(result.contains(u1.getId()));
    assertTrue(result.contains(u2.getId()));
  }

  @Test
  void getUnitSettlementItems_shouldReturnDtos() {
    UUID unitId = UUID.randomUUID();

    UnitSettlementItem item = new UnitSettlementItem();
    item.setId(UUID.randomUUID());
    item.setUnitId(unitId);
    item.setSettlementItemType(SettlementItemType.DEPOSIT);
    item.setPricePerUnit(BigDecimal.TEN);
    item.setMeasurementUnit(null);
    item.setBillingType(BillingType.FIXED);

    when(unitSettlementItemRepository.findByUnitId(unitId)).thenReturn(List.of(item));

    var result = propertyService.getUnitSettlementItems(unitId);

    assertEquals(1, result.size());

    UnitSettlementItemDto dto = result.get(0);
    assertEquals(item.getId(), dto.id());
    assertEquals(unitId, dto.unitId());
    assertEquals(SettlementItemType.DEPOSIT, dto.settlementItemType());
    assertEquals(BigDecimal.TEN, dto.pricePerUnit());
    assertNull(dto.measurementUnit());
    assertEquals(BillingType.FIXED, dto.billingType());
  }

  @Test
  void getUnitSettlementItems_shouldReturnEmptyList() {
    UUID unitId = UUID.randomUUID();

    when(unitSettlementItemRepository.findByUnitId(unitId)).thenReturn(List.of());

    var result = propertyService.getUnitSettlementItems(unitId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getUnitById_shouldConvertCurrencyWhenProvided() {
    UUID id = UUID.randomUUID();
    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

    io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto rateDto =
        new io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto(
            "no", java.time.LocalDate.now(), BigDecimal.valueOf(4.0));
    io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto responseDto =
        new io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto(
            "A", "EUR", "code", List.of(rateDto));

    when(nbpExchangeRateClient.getEurExchangeRate()).thenReturn(responseDto);

    var result = propertyService.getUnitById(id, "EUR");

    assertEquals(BigDecimal.valueOf(50).setScale(2), result.getConvertedPricePerNight());
    assertEquals("EUR", result.getCurrencyInfo().displayCurrency());
  }

  @Test
  void getUnitById_shouldFallbackToPlnOnClientError() {
    UUID id = UUID.randomUUID();
    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
    when(nbpExchangeRateClient.getEurExchangeRate()).thenThrow(new RuntimeException("API error"));

    var result = propertyService.getUnitById(id, "EUR");

    assertEquals(BigDecimal.valueOf(200), result.getConvertedPricePerNight());
    assertEquals("PLN", result.getCurrencyInfo().displayCurrency());
  }

  @Test
  void getUnitById_shouldConvertUsdCurrencyWhenProvided() {
    UUID id = UUID.randomUUID();
    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

    io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto rateDto =
        new io.github.kwatera_project.kwatera.property_service.dto.NbpRateDto(
            "no", java.time.LocalDate.now(), BigDecimal.valueOf(4.0));
    io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto responseDto =
        new io.github.kwatera_project.kwatera.property_service.dto.NbpResponseDto(
            "A", "USD", "code", List.of(rateDto));

    when(nbpExchangeRateClient.getUsdExchangeRate()).thenReturn(responseDto);

    var result = propertyService.getUnitById(id, "USD");

    assertEquals(BigDecimal.valueOf(50).setScale(2), result.getConvertedPricePerNight());
    assertEquals("USD", result.getCurrencyInfo().displayCurrency());
  }

  @Test
  void getUnitById_shouldThrowBadRequestForUnsupportedCurrency() {
    UUID id = UUID.randomUUID();
    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> propertyService.getUnitById(id, "GBP"));

    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Unsupported currency", ex.getReason());
  }

  @Test
  void getUnitsForOwnerProperty_ShouldReturnUnits_WhenOwnerMatches() {
    // Given
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setPricePerNight(new BigDecimal("400.00"));

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit));
    when(unitImageRepository.findByUnitIdAndIsMainTrue(any())).thenReturn(Optional.empty());

    // When
    List<UnitDto> result = propertyService.getUnitsForOwnerProperty(ownerId, propertyId, "PLN");

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(unitRepository).findByPropertyId(propertyId);
  }

  @Test
  void getUnitsForOwnerProperty_ShouldThrowForbidden_WhenOwnerDoesNotMatch() {
    // Given
    UUID realOwnerId = UUID.randomUUID();
    UUID wrongOwnerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(realOwnerId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

    // When & Then
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.getUnitsForOwnerProperty(wrongOwnerId, propertyId, "PLN"));
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("Access denied", exception.getReason());
  }

  @Test
  void getUnitsForOwnerProperty_ShouldThrowNotFound_WhenPropertyDoesNotExist() {
    // Given
    UUID propertyId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId)).thenReturn(Optional.empty());

    // When & Then
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.getUnitsForOwnerProperty(UUID.randomUUID(), propertyId, "PLN"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void updateUnit_shouldUpdateFields_whenValidRequest() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(unitId);

    UnitUpdateRequest request = mock(UnitUpdateRequest.class);
    when(request.name()).thenReturn(Optional.of("New name"));
    when(request.description()).thenReturn(Optional.empty());
    when(request.pricePerNight()).thenReturn(Optional.of(BigDecimal.valueOf(200)));

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(unitRepository.save(any())).thenReturn(unit);

    UnitDto result = propertyService.updateUnit(ownerId, propertyId, unitId, "PLN", request);

    assertEquals("New name", unit.getName());
    assertEquals(BigDecimal.valueOf(200), unit.getPricePerNight());
    assertNotNull(result);

    verify(unitRepository).save(unit);
  }

  @Test
  void updateUnit_shouldThrow404_whenPropertyNotFound() {
    when(propertyRepository.findById(any())).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                propertyService.updateUnit(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "PLN",
                    mock(UnitUpdateRequest.class)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateUnit_shouldThrow403_whenNotOwner() {
    Property property = new Property();
    property.setOwnerId(UUID.randomUUID());

    when(propertyRepository.findById(any())).thenReturn(Optional.of(property));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                propertyService.updateUnit(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "PLN",
                    mock(UnitUpdateRequest.class)));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void updateProperty_shouldCallGeocoding_whenAddressChanged() {
    UUID ownerId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    PropertyUpdateRequest request = mock(PropertyUpdateRequest.class);

    when(request.city()).thenReturn(Optional.of("Warsaw"));
    when(request.country()).thenReturn(Optional.empty());
    when(request.postalCode()).thenReturn(Optional.empty());
    when(request.street()).thenReturn(Optional.empty());
    when(request.streetNumber()).thenReturn(Optional.empty());

    when(propertyRepository.findById(any())).thenReturn(Optional.of(property));
    when(geocodingService.getCoordinates(any(), any(), any(), any(), any()))
        .thenReturn(new Coordinates(new BigDecimal("52.1"), new BigDecimal("21.0")));

    propertyService.updateProperty(ownerId, UUID.randomUUID(), request);

    verify(geocodingService).getCoordinates(any(), any(), any(), any(), any());
  }

  @Test
  void deleteUnit_shouldThrow409_whenHasReservations() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(unitId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(reservationClient.hasReservationsForUnit(unitId, "token")).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.deleteUnit(ownerId, propertyId, unitId, "token"));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void deleteProperty_shouldThrow409_whenAnyUnitHasReservations() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit));
    when(reservationClient.hasReservationsForUnit(unit.getId(), "token")).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.deleteProperty(ownerId, propertyId, "token"));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void createProperty_shouldSaveAndReturnDto() {
    UUID ownerId = UUID.randomUUID();

    PropertyCreateRequest request = mock(PropertyCreateRequest.class);
    when(request.street()).thenReturn("Main");
    when(request.streetNumber()).thenReturn("1");
    when(request.postalCode()).thenReturn("00-001");
    when(request.city()).thenReturn("Warsaw");
    when(request.country()).thenReturn("PL");
    when(request.title()).thenReturn("Test");

    when(geocodingService.getCoordinates(any(), any(), any(), any(), any()))
        .thenReturn(new Coordinates(new BigDecimal("10.0"), new BigDecimal("20.0")));

    when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PropertyDto result = propertyService.createProperty(ownerId, request);

    assertNotNull(result);
    verify(geocodingService).getCoordinates(any(), any(), any(), any(), any());
    verify(propertyRepository).save(any(Property.class));
  }

  @Test
  void createUnit_shouldSaveUnit_whenValidData() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    UnitCreateRequest request = mock(UnitCreateRequest.class);

    when(request.name()).thenReturn("Room");
    when(request.pricePerNight()).thenReturn(BigDecimal.TEN);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UnitDto result = propertyService.createUnit(ownerId, propertyId, request);

    assertNotNull(result);
    verify(unitRepository).save(any(Unit.class));
  }

  @Test
  void uploadPropertyImage_shouldSaveImageAndClearMain_whenValidAndIsMain() throws IOException {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    MockMultipartFile file =
        new MockMultipartFile("file", "image.jpg", "image/jpeg", "content".getBytes());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
    mockedFiles
        .when(
            () ->
                Files.copy(any(InputStream.class), any(Path.class), any(StandardCopyOption.class)))
        .thenReturn(0L);

    assertDoesNotThrow(() -> propertyService.uploadPropertyImage(ownerId, propertyId, true, file));

    verify(propertyImageRepository).clearMainImage(propertyId);
    verify(propertyImageRepository).save(any(PropertyImage.class));
  }

  @Test
  void uploadPropertyImage_shouldThrowBadRequest_whenFileIsEmpty() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.uploadPropertyImage(ownerId, propertyId, false, file));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("File is empty or invalid", ex.getReason());
  }

  @Test
  void uploadPropertyImage_shouldThrowForbidden_whenUserIsNotOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID wrongOwnerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(wrongOwnerId);

    MockMultipartFile file =
        new MockMultipartFile("file", "image.png", "image/png", "content".getBytes());
    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.uploadPropertyImage(ownerId, propertyId, false, file));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void uploadPropertyImage_shouldThrowBadRequest_whenInvalidExtension() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    MockMultipartFile file =
        new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes());
    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.uploadPropertyImage(ownerId, propertyId, false, file));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid image format", ex.getReason());
  }

  @Test
  void uploadUnitImage_shouldSaveImage_whenValidData() throws IOException {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(unitId);

    MockMultipartFile file =
        new MockMultipartFile("file", "room.jpeg", "image/jpeg", "content".getBytes());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
    mockedFiles
        .when(
            () ->
                Files.copy(any(InputStream.class), any(Path.class), any(StandardCopyOption.class)))
        .thenReturn(0L);

    assertDoesNotThrow(
        () -> propertyService.uploadUnitImage(ownerId, propertyId, unitId, false, file));

    verify(unitImageRepository, never()).clearMainImage(any());
    verify(unitImageRepository).save(any(UnitImage.class));
  }

  @Test
  void uploadUnitImage_shouldThrowNotFound_whenUnitDoesNotExist() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    MockMultipartFile file =
        new MockMultipartFile("file", "room.png", "image/png", "content".getBytes());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.uploadUnitImage(ownerId, propertyId, unitId, false, file));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Unit not found", ex.getReason());
  }

  @Test
  void deletePropertyImage_shouldDeleteFileAndRecord_whenValid() throws IOException {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    PropertyImage propertyImage = new PropertyImage();
    propertyImage.setPropertyId(propertyId);
    propertyImage.setUrl("http://localhost:8083/properties/" + propertyId + "/photo.jpg");

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(propertyImageRepository.findById(imageId)).thenReturn(Optional.of(propertyImage));
    mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

    assertDoesNotThrow(() -> propertyService.deletePropertyImage(ownerId, propertyId, imageId));

    verify(propertyImageRepository).delete(propertyImage);
  }

  @Test
  void deletePropertyImage_shouldThrowBadRequest_whenImageDoesNotBelongToProperty() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    PropertyImage propertyImage = new PropertyImage();
    propertyImage.setPropertyId(UUID.randomUUID());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(propertyImageRepository.findById(imageId)).thenReturn(Optional.of(propertyImage));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.deletePropertyImage(ownerId, propertyId, imageId));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Image does not belong to this property", ex.getReason());
  }

  @Test
  void deleteUnitImage_shouldDeleteFileAndRecord_whenValid() throws IOException {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();

    UnitImage unitImage = new UnitImage();
    unitImage.setUnitId(unitId);
    unitImage.setUrl("http://localhost:8083/properties/units/photo.png");

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(unitImageRepository.findById(imageId)).thenReturn(Optional.of(unitImage));
    mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

    assertDoesNotThrow(() -> propertyService.deleteUnitImage(ownerId, propertyId, unitId, imageId));

    verify(unitImageRepository).delete(unitImage);
  }

  @Test
  void setPropertyImageAsMain_shouldUpdateIsMainAndClearOthers_whenIsMainTrue() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    PropertyImage propertyImage = new PropertyImage();
    propertyImage.setPropertyId(propertyId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(propertyImageRepository.findById(imageId)).thenReturn(Optional.of(propertyImage));

    propertyService.setPropertyImageAsMain(ownerId, propertyId, imageId, true);

    assertTrue(propertyImage.getIsMain());
    verify(propertyImageRepository).clearMainImage(propertyId);
    verify(propertyImageRepository).save(propertyImage);
  }

  @Test
  void setUnitImageAsMain_shouldUpdateIsMainAndClearOthers_whenIsMainTrue() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();

    UnitImage unitImage = new UnitImage();
    unitImage.setUnitId(unitId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(unitImageRepository.findById(imageId)).thenReturn(Optional.of(unitImage));

    propertyService.setUnitImageAsMain(ownerId, propertyId, unitId, imageId, true);

    assertTrue(unitImage.getIsMain());
    verify(unitImageRepository).clearMainImage(unitId);
    verify(unitImageRepository).save(unitImage);
  }

  @Test
  void deleteUnit_shouldDeleteUnit_whenValidRequestAndNoReservations() {
    // Given
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String token = "valid-token";

    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    Unit unit = new Unit();
    unit.setId(unitId);
    unit.setPropertyId(propertyId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(reservationClient.hasReservationsForUnit(unitId, token)).thenReturn(false);

    // When
    assertDoesNotThrow(() -> propertyService.deleteUnit(ownerId, propertyId, unitId, token));

    // Then - Sprawdzenie, że metoda usuwająca z repozytorium została wywołana dokładnie raz
    verify(unitRepository, times(1)).delete(unit);
  }

  @Test
  void deleteProperty_shouldDeletePropertyAndUnits_whenValidRequestAndNoReservations() {
    // Given
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    String token = "valid-token";

    Property property = new Property();
    property.setId(propertyId);
    property.setOwnerId(ownerId);

    Unit unit1 = new Unit();
    unit1.setId(UUID.randomUUID());

    Unit unit2 = new Unit();
    unit2.setId(UUID.randomUUID());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit1, unit2));
    when(reservationClient.hasReservationsForUnit(unit1.getId(), token)).thenReturn(false);
    when(reservationClient.hasReservationsForUnit(unit2.getId(), token)).thenReturn(false);

    // When
    assertDoesNotThrow(() -> propertyService.deleteProperty(ownerId, propertyId, token));

    // Then
    verify(unitRepository, times(1)).deleteByPropertyId(propertyId);
    verify(propertyRepository, times(1)).delete(property);
  }

  @Test
  void getUnits_shouldThrowNotFound_whenPropertyDoesNotExist() {
    UUID propertyId = UUID.randomUUID();
    when(propertyRepository.existsById(propertyId)).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> propertyService.getUnits(propertyId, "PLN"));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Property not found", ex.getReason());
  }

  @Test
  void getUnits_shouldThrowBadRequest_whenCurrencyIsUnsupported() {
    UUID propertyId = UUID.randomUUID();
    when(propertyRepository.existsById(propertyId)).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> propertyService.getUnits(propertyId, "CHF"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Unsupported currency", ex.getReason());
  }

  @Test
  void getUnitImages_shouldThrowNotFound_whenUnitDoesNotExistOrNotBelongToProperty() {
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> propertyService.getUnitImages(propertyId, unitId));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Unit not found", ex.getReason());
  }

  @Test
  void createUnit_shouldThrowForbidden_whenUserIsNotPropertyOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID wrongOwnerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(wrongOwnerId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.createUnit(ownerId, propertyId, mock(UnitCreateRequest.class)));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    assertEquals("Access denied", ex.getReason());
  }

  @Test
  void deletePropertyImage_shouldThrowNotFound_whenImageDoesNotExist() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(propertyImageRepository.findById(imageId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.deletePropertyImage(ownerId, propertyId, imageId));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Image not found", ex.getReason());
  }

  @Test
  void deleteUnitImage_shouldThrowBadRequest_whenImageDoesNotBelongToUnit() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();

    UnitImage unitImage = new UnitImage();
    unitImage.setUnitId(UUID.randomUUID()); // Inne unitId niż żądane

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(unitImageRepository.findById(imageId)).thenReturn(Optional.of(unitImage));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.deleteUnitImage(ownerId, propertyId, unitId, imageId));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Image does not belong to this unit", ex.getReason());
  }

  @Test
  void setPropertyImageAsMain_shouldThrowBadRequest_whenImageDoesNotBelongToProperty() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    PropertyImage propertyImage = new PropertyImage();
    propertyImage.setPropertyId(UUID.randomUUID());

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(propertyImageRepository.findById(imageId)).thenReturn(Optional.of(propertyImage));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.setPropertyImageAsMain(ownerId, propertyId, imageId, true));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Image does not belong to this property", ex.getReason());
  }

  @Test
  void setUnitImageAsMain_shouldThrowNotFound_whenImageDoesNotExist() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    Property property = new Property();
    property.setOwnerId(ownerId);

    Unit unit = new Unit();

    when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    when(unitRepository.findByIdAndPropertyId(unitId, propertyId)).thenReturn(Optional.of(unit));
    when(unitImageRepository.findById(imageId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> propertyService.setUnitImageAsMain(ownerId, propertyId, unitId, imageId, true));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Image not found", ex.getReason());
  }
}
