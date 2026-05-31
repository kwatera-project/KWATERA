package io.github.kwatera_project.kwatera.billing_service.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.dto.OcrResponseDto;
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
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import io.github.kwatera_project.kwatera.billing_service.service.SettlementService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

public class BillingFlow {

  @Autowired private SettlementService settlementService;
  @Autowired private MediaReadingService mediaReadingService;
  @Autowired private SettlementRepository settlementRepository;
  @Autowired private SettlementItemRepository settlementItemRepository;
  @Autowired private MediaReadingRepository mediaReadingRepository;
  @Autowired private MediaReadingUploadAttemptRepository uploadAttemptRepository;
  @Autowired private OcrClient ocrClient;

  private UUID settlementId;
  private UUID unitId;
  private MultipartFile multipartFile;
  private ReadingStatus lastReadingStatus;
  private Exception lastException;

  // Prepare clean test data before each scenario
  @Before
  public void setUp() throws Exception {
    uploadAttemptRepository.deleteAll();
    mediaReadingRepository.deleteAll();
    settlementItemRepository.deleteAll();
    settlementRepository.deleteAll();

    unitId = UUID.randomUUID();

    multipartFile = mock(MultipartFile.class);
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReflectionTestUtils.setField(
        mediaReadingService, "ocrConfidenceThreshold", new BigDecimal("0.70"));
  }

  // Create settlement for an existing reservation
  @Given("a reservation exists with accommodation amount {double} PLN")
  public void aReservationExistsWithAccommodationAmount(double amount) {
    Settlement settlement =
        settlementService.createSettlement(UUID.randomUUID(), BigDecimal.valueOf(amount));

    settlementId = settlement.getId();

    assertThat(settlementId).isNotNull();
  }

  // Add approved initial water meter reading
  @Given("the initial water meter reading was approved with value {int}")
  public void theInitialWaterMeterReadingWasApprovedWithValue(int value) {
    MediaReading reading = new MediaReading();

    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReading(BigDecimal.valueOf(value));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setInitialReadingSource(ReadingSource.OCR);
    reading.setInitialConfidenceScore(new BigDecimal("0.95"));
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    mediaReadingRepository.save(reading);
  }

  // Add water utility charge to existing settlement
  @Given("a water utility charge of {double} PLN has been added to the settlement")
  public void aWaterUtilityChargeHasBeenAddedToTheSettlement(double amount) {
    settlementService.addUtilitySettlementItem(
        settlementId,
        unitId,
        SettlementItemType.WATER,
        "Water usage",
        new BigDecimal("50"),
        new BigDecimal("5.00"));
  }

  // This step represents guest photo upload
  @When("the guest uploads a final water meter photo")
  public void theGuestUploadsAFinalWaterMeterPhoto() {
    // The upload is processed in the next step after OCR response is mocked
  }

  // Mock accepted OCR response and process final meter reading
  @And("the OCR reads the meter value as {int} with confidence {double}")
  public void theOcrReadsTheMeterValueAsWithConfidence(int value, double confidence)
      throws Exception {
    when(ocrClient.readMeter(any()))
        .thenReturn(new OcrResponseDto(String.valueOf(value), BigDecimal.valueOf(confidence)));

    lastReadingStatus =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);
  }

  // Mock invalid OCR response and process final meter reading
  @And("the OCR returns an invalid meter value")
  public void theOcrReturnsAnInvalidMeterValue() throws Exception {
    when(ocrClient.readMeter(any()))
        .thenReturn(new OcrResponseDto("not-a-number", new BigDecimal("0.95")));

    lastReadingStatus =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);
  }

  // Register accommodation payment for the settlement
  @When("the guest pays {double} PLN for accommodation")
  public void theGuestPaysForAccommodation(double amount) {
    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.ACCOMMODATION,
        "Accommodation payment",
        BigDecimal.ONE,
        BigDecimal.valueOf(amount));
  }

  // Try to add the same water charge again
  @When("a duplicate water utility charge is submitted")
  public void aDuplicateWaterUtilityChargeIsSubmitted() {
    try {
      settlementService.addUtilitySettlementItem(
          settlementId,
          unitId,
          SettlementItemType.WATER,
          "Water usage",
          new BigDecimal("50"),
          new BigDecimal("5.00"));
    } catch (IllegalStateException e) {
      lastException = e;
    }
  }

  // Check final reading status after OCR processing
  @Then("the final reading status should be {word}")
  public void theFinalReadingStatusShouldBe(String status) {
    assertThat(lastReadingStatus).isEqualTo(ReadingStatus.valueOf(status));
  }

  // Check if water charge was added correctly
  @And("a water utility charge of {double} PLN should be added to the settlement")
  public void aWaterUtilityChargeShouldBeAddedToTheSettlement(double amount) {
    List<SettlementItem> items = settlementItemRepository.findBySettlementId(settlementId);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getType()).isEqualTo(SettlementItemType.WATER);
    assertThat(items.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(amount));
  }

  // Check settlement total amount
  @And("the settlement total should be {double} PLN")
  public void theSettlementTotalShouldBe(double total) {
    Settlement settlement = findSettlement();

    assertThat(settlement.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(total));
  }

  // Check remaining amount to pay
  @And("the settlement balance due should be {double} PLN")
  public void theSettlementBalanceDueShouldBe(double balance) {
    Settlement settlement = findSettlement();

    assertThat(settlement.getBalanceDue()).isEqualByComparingTo(BigDecimal.valueOf(balance));
  }

  // Check that no utility charge was added
  @And("no utility charge should be added to the settlement")
  public void noUtilityChargeShouldBeAddedToTheSettlement() {
    assertThat(settlementItemRepository.findBySettlementId(settlementId)).isEmpty();
  }

  // Check already paid amount
  @And("the settlement amount paid should be {double} PLN")
  public void theSettlementAmountPaidShouldBe(double amount) {
    Settlement settlement = findSettlement();

    assertThat(settlement.getAmountPaid()).isEqualByComparingTo(BigDecimal.valueOf(amount));
  }

  // Check settlement status
  @And("the settlement status should be {word}")
  public void theSettlementStatusShouldBe(String status) {
    Settlement settlement = findSettlement();

    assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.valueOf(status));
  }

  // Check if duplicate charge was rejected
  @Then("the system should reject it with an error")
  public void theSystemShouldRejectItWithAnError() {
    assertThat(lastException).isInstanceOf(IllegalStateException.class);
    assertThat(lastException.getMessage()).contains("Utility charge already exists");
  }

  // Find current settlement from database
  private Settlement findSettlement() {
    return settlementRepository.findById(settlementId).orElseThrow();
  }
}
