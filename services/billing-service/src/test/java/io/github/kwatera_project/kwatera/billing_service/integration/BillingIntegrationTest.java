package io.github.kwatera_project.kwatera.billing_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.dto.OcrResponseDto;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingSource;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingUploadAttemptRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementItemRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import io.github.kwatera_project.kwatera.billing_service.service.EmailNotificationService;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import io.github.kwatera_project.kwatera.billing_service.service.SettlementService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Use Testcontainers to run PostgreSQL for tests
@Testcontainers
// Run test with full Spring context
@SpringBootTest(
    properties = {
      "spring.cloud.discovery.enabled=false",
      "eureka.client.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false"
    })
class BillingIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("billing_test")
          .withUsername("test")
          .withPassword("test");

  // Connect Spring Boot test context to PostgreSQL container
  @DynamicPropertySource
  static void configurePostgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }

  @Autowired private SettlementService settlementService;
  @Autowired private MediaReadingService mediaReadingService;

  @Autowired private SettlementRepository settlementRepository;
  @Autowired private SettlementItemRepository settlementItemRepository;
  @Autowired private MediaReadingRepository mediaReadingRepository;
  @Autowired private MediaReadingUploadAttemptRepository uploadAttemptRepository;

  @MockitoBean private SettlementEventPublisher settlementEventPublisher;
  @MockitoBean private EmailNotificationService emailNotificationService;
  @MockitoBean private OcrClient ocrClient;
  @MockitoBean private PropertyClient propertyClient;

  private MultipartFile multipartFile;

  @BeforeEach
  void setUp() throws Exception {
    // Clean database before each test
    uploadAttemptRepository.deleteAll();
    mediaReadingRepository.deleteAll();
    settlementItemRepository.deleteAll();
    settlementRepository.deleteAll();

    multipartFile = mock(MultipartFile.class);
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReflectionTestUtils.setField(
        mediaReadingService, "ocrConfidenceThreshold", new BigDecimal("0.70"));
  }

  // Checks if a settlement is created with base amounts.
  @Test
  void shouldCreateSettlement() {
    UUID reservationId = UUID.randomUUID();

    Settlement settlement =
        settlementService.createSettlement(
            reservationId, new BigDecimal("800.00"), new BigDecimal("200.00"));

    assertThat(settlement.getId()).isNotNull();
    assertThat(settlement.getReservationId()).isEqualTo(reservationId);
    assertThat(settlement.getAccommodationAmount()).isEqualByComparingTo("800.00");
    assertThat(settlement.getDepositAmount()).isEqualByComparingTo("200.00");
    assertThat(settlement.getTotalAmount()).isEqualByComparingTo("1000.00");
    assertThat(settlement.getAmountPaid()).isEqualByComparingTo("0.00");
    assertThat(settlement.getBalanceDue()).isEqualByComparingTo("1000.00");
    assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DRAFT);
  }

  // Check if a settlement is saved in PostgreSQL
  @Test
  void shouldSaveSettlementInDatabase() {
    UUID reservationId = UUID.randomUUID();

    Settlement settlement =
        settlementService.createSettlement(reservationId, new BigDecimal("500.00"));

    Settlement fromDb = findSettlement(settlement.getId());

    assertThat(fromDb.getReservationId()).isEqualTo(reservationId);
    assertThat(fromDb.getTotalAmount()).isEqualByComparingTo("500.00");
  }

  // Checks if utility cost is added to settlement
  @Test
  void shouldAddUtilityCharge() {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    addWaterCharge(settlement.getId(), unitId);

    assertSettlementAmounts(settlement.getId(), "250.00", "750.00", "0.00", "750.00");
    assertWaterCharge(settlement.getId());
  }

  // Check if payment updates amount paid and balance due
  @Test
  void shouldUpdateBalanceAfterPayment() {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    addWaterCharge(settlement.getId(), unitId);

    settlementService.registerPayment(
        settlement.getId(),
        unitId,
        SettlementItemType.ACCOMMODATION,
        "Accommodation payment",
        BigDecimal.ONE,
        new BigDecimal("500.00"));

    assertSettlementAmounts(settlement.getId(), "250.00", "750.00", "500.00", "250.00");
  }

  // Check if duplicate utility charge is blocked
  @Test
  void shouldBlockDuplicateUtilityCharge() {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    addWaterCharge(settlement.getId(), unitId);

    assertThatThrownBy(() -> addWaterCharge(settlement.getId(), unitId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Utility charge already exists");
  }

  // Check if accepted OCR reading adds utility charge
  @Test
  void shouldAddChargeAfterAcceptedOcrReading() throws Exception {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    createApprovedInitialReading(settlement.getId(), "100.00", "5.00");
    mockOcr("150.00", "0.95");

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlement.getId(), unitId, UtilityType.WATER, multipartFile);

    assertThat(status).isEqualTo(ReadingStatus.AUTO_APPROVED);

    MediaReading reading = findWaterReading(settlement.getId());

    assertThat(reading.getFinalReading()).isEqualByComparingTo("150.00");
    assertThat(reading.getFinalReadingStatus()).isEqualTo(ReadingStatus.AUTO_APPROVED);
    assertThat(reading.getFinalReadingSource()).isEqualTo(ReadingSource.OCR);

    assertWaterCharge(settlement.getId());
    assertSettlementAmounts(settlement.getId(), "250.00", "750.00", "0.00", "750.00");
  }

  // Check if invalid OCR value does not change settlement
  @Test
  void shouldIgnoreInvalidOcrReading() throws Exception {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    createApprovedInitialReading(settlement.getId(), "100.00", "5.00");
    mockOcr("bad-value", "0.95");

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlement.getId(), unitId, UtilityType.WATER, multipartFile);

    assertThat(status).isEqualTo(ReadingStatus.REQUEST_REUPLOAD);

    assertReadingNeedsReupload(settlement.getId());
    assertNoUtilityCharge(settlement.getId());
  }

  // Check if OCR result with low confidence does not change settlement
  @Test
  void shouldIgnoreLowConfidenceOcrReading() throws Exception {
    Settlement settlement = createBaseSettlement();
    UUID unitId = UUID.randomUUID();

    createApprovedInitialReading(settlement.getId(), "100.00", "5.00");
    mockOcr("150.00", "0.50");

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlement.getId(), unitId, UtilityType.WATER, multipartFile);

    assertThat(status).isEqualTo(ReadingStatus.REQUEST_REUPLOAD);

    assertReadingNeedsReupload(settlement.getId());
    assertNoUtilityCharge(settlement.getId());
  }

  // Create a basic settlement with accommodation amount 500.00
  private Settlement createBaseSettlement() {
    return settlementService.createSettlement(UUID.randomUUID(), new BigDecimal("500.00"));
  }

  // Find settlement in database by id
  private Settlement findSettlement(UUID settlementId) {
    return settlementRepository.findById(settlementId).orElseThrow();
  }

  // Find water media reading for settlement
  private MediaReading findWaterReading(UUID settlementId) {
    return mediaReadingRepository
        .findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER)
        .orElseThrow();
  }

  // Add water charge 50 units * 5.00
  private void addWaterCharge(UUID settlementId, UUID unitId) {
    settlementService.addUtilitySettlementItem(
        settlementId,
        unitId,
        SettlementItemType.WATER,
        "Water usage",
        new BigDecimal("50"),
        new BigDecimal("5.00"));
  }

  // Mock ocr response returned by ocr client
  private void mockOcr(String value, String confidence) throws Exception {
    when(ocrClient.readMeter(any(MultipartFile.class)))
        .thenReturn(new OcrResponseDto(value, new BigDecimal(confidence)));
  }

  // Created approved initial water reading before final reading upload
  private void createApprovedInitialReading(
      UUID settlementId, String initialReading, String unitPrice) {
    MediaReading reading = new MediaReading();

    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReading(new BigDecimal(initialReading));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setInitialReadingSource(ReadingSource.OCR);
    reading.setInitialConfidenceScore(new BigDecimal("0.95"));
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal(unitPrice));

    mediaReadingRepository.save(reading);
  }

  // Check main settlement amount
  private void assertSettlementAmounts(
      UUID settlementId,
      String utilitiesAmount,
      String totalAmount,
      String amountPaid,
      String balanceDue) {
    Settlement settlement = findSettlement(settlementId);

    assertThat(settlement.getAccommodationAmount()).isEqualByComparingTo("500.00");
    assertThat(settlement.getUtilitiesAmount()).isEqualByComparingTo(utilitiesAmount);
    assertThat(settlement.getTotalAmount()).isEqualByComparingTo(totalAmount);
    assertThat(settlement.getAmountPaid()).isEqualByComparingTo(amountPaid);
    assertThat(settlement.getBalanceDue()).isEqualByComparingTo(balanceDue);
  }

  // Check if one correct water charge was added
  private void assertWaterCharge(UUID settlementId) {
    List<SettlementItem> items = settlementItemRepository.findBySettlementId(settlementId);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getType()).isEqualTo(SettlementItemType.WATER);
    assertThat(items.get(0).getQuantity()).isEqualByComparingTo("50.00");
    assertThat(items.get(0).getUnitPrice()).isEqualByComparingTo("5.00");
    assertThat(items.get(0).getAmount()).isEqualByComparingTo("250.00");
  }

  // Check if no utility charge was added and settlement stayed unchanged
  private void assertNoUtilityCharge(UUID settlementId) {
    assertThat(settlementItemRepository.findBySettlementId(settlementId)).isEmpty();
    assertSettlementAmounts(settlementId, "0.00", "500.00", "0.00", "500.00");
  }

  // Check if reading needs reupload.
  private void assertReadingNeedsReupload(UUID settlementId) {
    MediaReading reading = findWaterReading(settlementId);

    assertThat(reading.getFinalReading()).isNull();
    assertThat(reading.getFinalReadingStatus()).isEqualTo(ReadingStatus.REQUEST_REUPLOAD);
  }
}
