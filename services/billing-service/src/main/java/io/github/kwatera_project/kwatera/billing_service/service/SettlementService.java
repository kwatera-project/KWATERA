package io.github.kwatera_project.kwatera.billing_service.service;

import static io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType.*;
import static io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.*;

import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.client.SystemEventClient;
import io.github.kwatera_project.kwatera.billing_service.dto.*;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.model.PaymentTransaction;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementItemRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SettlementService {

  private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

  private final SettlementRepository settlementRepository;
  private final SettlementItemRepository settlementItemRepository;
  private final SettlementEventPublisher settlementEventPublisher;
  private final PropertyClient propertyClient;
  private final EmailNotificationService emailNotificationService;
  private final SystemEventClient systemEventClient;
  private final io.github.kwatera_project.kwatera.billing_service.client.ReservationClient
      reservationClient;
  private final org.thymeleaf.TemplateEngine templateEngine;
  private final io.github.kwatera_project.kwatera.billing_service.repository
          .PaymentTransactionRepository
      paymentTransactionRepository;

  private static final String SETTLEMENT_NOT_FOUND = "Settlement not found";

  @Transactional
  public void registerPayment(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {
    registerPayment(settlementId, unitId, type, description, quantity, unitPrice, null);
  }

  @Transactional
  public void registerPayment(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String recipientEmail) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));
    BigDecimal previousBalance = zeroIfNull(settlement.getBalanceDue());

    SettlementItem item =
        createSettlementItem(settlementId, type, description, quantity, unitPrice);

    applyItemToSettlement(settlement, item);

    BigDecimal newPaid = settlement.getAmountPaid().add(item.getAmount());

    settlement.setAmountPaid(newPaid);

    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId, recipientEmail);

    if (newPaid.compareTo(settlement.getTotalAmount()) > 0) {
      throw new IllegalStateException("Payment exceeds settlement total");
    }

    settlementRepository.save(settlement);
    logBalanceChangedIfNeeded(
        settlement,
        previousBalance,
        "amountPaid="
            + settlement.getAmountPaid()
            + ", totalAmount="
            + settlement.getTotalAmount()
            + ", type="
            + type
            + ", itemAmount="
            + item.getAmount());
  }

  @Transactional
  public SettlementItem addUtilitySettlementItem(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {

    if (type != WATER && type != ELECTRICITY) {
      throw new IllegalArgumentException(
          "Only water and electricity utility charges are supported");
    }
    if (settlementItemRepository.findBySettlementIdAndType(settlementId, type).isPresent()) {
      throw new IllegalStateException(
          "Utility charge already exists for settlementId: " + settlementId + " and type: " + type);
    }

    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));
    BigDecimal previousBalance = zeroIfNull(settlement.getBalanceDue());

    SettlementItem item =
        createSettlementItem(settlementId, type, description, quantity, unitPrice);

    applyItemToSettlement(settlement, item);
    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId, null);

    settlementRepository.save(settlement);
    logMediaSettlementGenerated(settlement, item, unitId, previousBalance);
    logBalanceChangedIfNeeded(
        settlement,
        previousBalance,
        "unitId="
            + unitId
            + ", type="
            + type
            + ", quantity="
            + quantity
            + ", unitPrice="
            + unitPrice
            + ", itemAmount="
            + item.getAmount());

    try {
      emailNotificationService.sendUtilityChargesAdded(settlement, item);
    } catch (Exception e) {
      log.warn("Failed to send utility charges notification for settlement {}", settlementId, e);
    }

    return item;
  }

  private void applyItemToSettlement(Settlement settlement, SettlementItem item) {

    BigDecimal amount = item.getAmount();

    switch (item.getType()) {
      case DEPOSIT -> settlement.setDepositAmount(settlement.getDepositAmount().add(amount));

      case ELECTRICITY, WATER, CLEANING_FEE ->
          settlement.setUtilitiesAmount(settlement.getUtilitiesAmount().add(amount));

      case ACCOMMODATION -> {}
    }
  }

  private SettlementStatus calculateStatus(Settlement settlement, boolean hasRequiredItems) {

    BigDecimal balance = settlement.getTotalAmount().subtract(settlement.getAmountPaid());

    if (balance.compareTo(BigDecimal.ZERO) <= 0 && hasRequiredItems) {
      return PAID;
    }

    boolean hasAdditionalCharges =
        settlementItemRepository.existsBySettlementIdAndTypeIn(
            settlement.getId(), List.of(ELECTRICITY, WATER, CLEANING_FEE));

    return hasAdditionalCharges ? ISSUED : PARTIALLY_PAID;
  }

  private void recalculateSettlementStatus(
      Settlement settlement, UUID unitId, String recipientEmail) {

    BigDecimal balance = settlement.getTotalAmount().subtract(settlement.getAmountPaid());

    settlement.setBalanceDue(balance.max(BigDecimal.ZERO));

    boolean hasRequiredItems;

    if (balance.compareTo(BigDecimal.ZERO) <= 0) {
      hasRequiredItems = hasAllRequiredItems(settlement, unitId);
    } else {
      hasRequiredItems = false;
    }

    SettlementStatus previous = settlement.getStatus();

    SettlementStatus newStatus = calculateStatus(settlement, hasRequiredItems);

    settlement.setStatus(newStatus);

    if (previous != newStatus) {
      settlementEventPublisher.publishSettlementStatusChanged(
          new SettlementStatusChangedEvent(settlement.getReservationId(), newStatus));
      emailNotificationService.sendPaymentStatusChanged(
          settlement, previous, newStatus, recipientEmail);
      try {
        emailNotificationService.sendOwnerPaymentStatusChanged(
            settlement, previous, newStatus, unitId);
      } catch (Exception e) {
        log.warn("Failed to send owner notification for payment status change", e);
      }
    }
  }

  private SettlementItem createSettlementItem(
      UUID settlementId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {

    SettlementItem item = new SettlementItem();

    item.setSettlementId(settlementId);
    item.setType(type);
    item.setDescription(description);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    BigDecimal amount = unitPrice.multiply(quantity);
    item.setAmount(amount);

    return settlementItemRepository.save(item);
  }

  @Transactional
  public Settlement createSettlement(UUID reservationId, BigDecimal accommodationAmount) {
    return createSettlement(reservationId, accommodationAmount, BigDecimal.ZERO, null);
  }

  @Transactional
  public Settlement createSettlement(
      UUID reservationId, BigDecimal accommodationAmount, String recipientEmail) {
    return createSettlement(reservationId, accommodationAmount, BigDecimal.ZERO, recipientEmail);
  }

  @Transactional
  public Settlement createSettlement(
      UUID reservationId, BigDecimal accommodationAmount, BigDecimal depositAmount) {
    return createSettlement(reservationId, accommodationAmount, depositAmount, null);
  }

  @Transactional
  public Settlement createSettlement(
      UUID reservationId,
      BigDecimal accommodationAmount,
      BigDecimal depositAmount,
      String recipientEmail) {
    Settlement settlement = new Settlement();

    settlement.setReservationId(reservationId);

    settlement.setAccommodationAmount(accommodationAmount);
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(depositAmount);
    settlement.setDiscountAmount(BigDecimal.ZERO);

    settlement.setAmountPaid(BigDecimal.ZERO);

    settlement.setStatus(DRAFT);

    recalculateTotals(settlement);

    Settlement saved = settlementRepository.save(settlement);
    emailNotificationService.sendSettlementCreated(saved, recipientEmail);
    systemEventClient.logSafely(
        "BALANCE_CHANGED",
        null,
        "SETTLEMENT",
        saved.getId(),
        "settlementId="
            + saved.getId()
            + ", reservationId="
            + saved.getReservationId()
            + ", previousBalance=0, newBalance="
            + saved.getBalanceDue()
            + ", totalAmount="
            + saved.getTotalAmount()
            + ", amountPaid="
            + saved.getAmountPaid());
    return saved;
  }

  private void recalculateTotals(Settlement settlement) {
    BigDecimal total =
        settlement
            .getAccommodationAmount()
            .add(settlement.getUtilitiesAmount())
            .add(settlement.getDepositAmount())
            .subtract(settlement.getDiscountAmount());

    if (total.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalStateException("Settlement total cannot be negative");
    }

    settlement.setTotalAmount(total);

    BigDecimal balance = total.subtract(settlement.getAmountPaid());
    settlement.setBalanceDue(balance.max(BigDecimal.ZERO));
  }

  @Transactional
  public void applyDiscount(UUID settlementId, UUID unitId, BigDecimal discountAmount) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));
    BigDecimal previousBalance = zeroIfNull(settlement.getBalanceDue());

    settlement.setDiscountAmount(discountAmount);

    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId, null);

    settlementRepository.save(settlement);
    logBalanceChangedIfNeeded(settlement, previousBalance, "discountAmount=" + discountAmount);
  }

  private void logMediaSettlementGenerated(
      Settlement settlement, SettlementItem item, UUID unitId, BigDecimal previousBalance) {
    UUID entityId = item.getId() != null ? item.getId() : settlement.getId();
    String entityType = item.getId() != null ? "SETTLEMENT_ITEM" : "SETTLEMENT";
    systemEventClient.logSafely(
        "MEDIA_SETTLEMENT_GENERATED",
        null,
        entityType,
        entityId,
        "settlementId="
            + settlement.getId()
            + ", unitId="
            + unitId
            + ", type="
            + item.getType()
            + ", quantity="
            + item.getQuantity()
            + ", unitPrice="
            + item.getUnitPrice()
            + ", itemAmount="
            + item.getAmount()
            + ", previousBalance="
            + previousBalance
            + ", newBalance="
            + settlement.getBalanceDue());
  }

  private void logBalanceChangedIfNeeded(
      Settlement settlement, BigDecimal previousBalance, String detailsSuffix) {
    BigDecimal newBalance = zeroIfNull(settlement.getBalanceDue());
    if (previousBalance.compareTo(newBalance) == 0) {
      return;
    }
    systemEventClient.logSafely(
        "BALANCE_CHANGED",
        null,
        "SETTLEMENT",
        settlement.getId(),
        "settlementId="
            + settlement.getId()
            + ", reservationId="
            + settlement.getReservationId()
            + ", previousBalance="
            + previousBalance
            + ", newBalance="
            + newBalance
            + ", "
            + detailsSuffix);
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  public SettlementResponseDto getSettlementWithItems(ReservationDto reservation) {

    Settlement settlement =
        settlementRepository
            .findByReservationId(reservation.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, SETTLEMENT_NOT_FOUND));

    List<SettlementItem> items = settlementItemRepository.findBySettlementId(settlement.getId());

    String rCurrency =
        (reservation.getCurrencyInfo() != null
                && reservation.getCurrencyInfo().displayCurrency() != null)
            ? reservation.getCurrencyInfo().displayCurrency()
            : "PLN";
    BigDecimal rRate =
        (reservation.getCurrencyInfo() != null
                && reservation.getCurrencyInfo().exchangeRate() != null
                && reservation.getCurrencyInfo().exchangeRate().compareTo(BigDecimal.ZERO) > 0)
            ? reservation.getCurrencyInfo().exchangeRate()
            : BigDecimal.ONE;

    CurrencyMetadataDto currencyInfo =
        new CurrencyMetadataDto("PLN", rCurrency, rRate, LocalDate.now());

    BigDecimal convertedTotalAmount = settlement.getTotalAmount();
    BigDecimal convertedAmountPaid = settlement.getAmountPaid();
    BigDecimal convertedBalanceDue = settlement.getBalanceDue();
    BigDecimal convertedAccommodationAmount = settlement.getAccommodationAmount();
    BigDecimal convertedUtilitiesAmount = settlement.getUtilitiesAmount();
    BigDecimal convertedDepositAmount = settlement.getDepositAmount();

    if (!"PLN".equalsIgnoreCase(rCurrency)) {
      if (convertedTotalAmount != null) {
        convertedTotalAmount = convertedTotalAmount.divide(rRate, 2, RoundingMode.HALF_UP);
      }
      if (convertedAmountPaid != null) {
        convertedAmountPaid = convertedAmountPaid.divide(rRate, 2, RoundingMode.HALF_UP);
      }
      if (convertedBalanceDue != null) {
        convertedBalanceDue = convertedBalanceDue.divide(rRate, 2, RoundingMode.HALF_UP);
      }
      if (convertedAccommodationAmount != null) {
        convertedAccommodationAmount =
            convertedAccommodationAmount.divide(rRate, 2, RoundingMode.HALF_UP);
      }
      if (convertedUtilitiesAmount != null) {
        convertedUtilitiesAmount = convertedUtilitiesAmount.divide(rRate, 2, RoundingMode.HALF_UP);
      }
      if (convertedDepositAmount != null) {
        convertedDepositAmount = convertedDepositAmount.divide(rRate, 2, RoundingMode.HALF_UP);
      }
    }

    SettlementDto dto =
        SettlementDto.from(
            settlement,
            convertedTotalAmount,
            convertedAmountPaid,
            convertedBalanceDue,
            convertedAccommodationAmount,
            convertedUtilitiesAmount,
            convertedDepositAmount,
            currencyInfo);
    return new SettlementResponseDto(dto, items);
  }

  public SettlementItemDto getSettlementItemInfoByType(
      ReservationDto reservation, SettlementItemType settlementItemType) {

    Settlement settlement =
        settlementRepository
            .findByReservationId(reservation.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, SETTLEMENT_NOT_FOUND));

    SettlementItem item =
        settlementItemRepository
            .findBySettlementIdAndType(settlement.getId(), settlementItemType)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement item not found"));

    String rCurrency =
        (reservation.getCurrencyInfo() != null
                && reservation.getCurrencyInfo().displayCurrency() != null)
            ? reservation.getCurrencyInfo().displayCurrency()
            : "PLN";
    BigDecimal rRate =
        (reservation.getCurrencyInfo() != null
                && reservation.getCurrencyInfo().exchangeRate() != null
                && reservation.getCurrencyInfo().exchangeRate().compareTo(BigDecimal.ZERO) > 0)
            ? reservation.getCurrencyInfo().exchangeRate()
            : BigDecimal.ONE;

    CurrencyMetadataDto currencyInfo =
        new CurrencyMetadataDto("PLN", rCurrency, rRate, LocalDate.now());
    BigDecimal convertedAmount = item.getAmount();

    if (!"PLN".equalsIgnoreCase(rCurrency)) {
      if (convertedAmount != null) {
        convertedAmount = convertedAmount.divide(rRate, 2, RoundingMode.HALF_UP);
      }
    }

    return SettlementItemDto.from(item, convertedAmount, currencyInfo);
  }

  private boolean hasAllRequiredItems(Settlement settlement, UUID unitId) {

    Set<SettlementItemType> requiredTypes =
        propertyClient.getUnitSettlementItems(unitId).stream()
            .map(UnitSettlementItemDto::settlementItemType)
            .collect(Collectors.toSet());

    Set<SettlementItemType> existingTypes =
        settlementItemRepository.findBySettlementId(settlement.getId()).stream()
            .map(SettlementItem::getType)
            .collect(Collectors.toSet());

    return existingTypes.containsAll(requiredTypes);
  }

  @Transactional
  public void generateInvoicePdfIfNeeded(UUID settlementId) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));

    if (!settlement.isInvoiceRequested()) {
      return;
    }

    try {
      ReservationDto reservation = reservationClient.getReservation(settlement.getReservationId());

      org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();

      String year = String.valueOf(LocalDate.now().getYear());
      String month = String.format("%02d", LocalDate.now().getMonthValue());
      String invoiceNumber =
          "FV/"
              + year
              + "/"
              + month
              + "/"
              + settlement.getId().toString().substring(0, 8).toUpperCase();
      context.setVariable("invoiceNumber", invoiceNumber);

      context.setVariable(
          "ownerName",
          reservation.getOwnerName() != null
              ? reservation.getOwnerName()
              : "Kwatera Property Management");
      context.setVariable(
          "ownerEmail",
          reservation.getOwnerEmail() != null
              ? reservation.getOwnerEmail()
              : "billing@kwatera.local");

      String guestName =
          reservation.getGuestName() != null
              ? reservation.getGuestName()
              : (reservation.getGuestEmail() != null ? reservation.getGuestEmail() : "Guest");
      context.setVariable(
          "buyerName",
          settlement.getCompanyName() != null && !settlement.getCompanyName().isBlank()
              ? settlement.getCompanyName()
              : guestName);
      context.setVariable("buyerTaxId", settlement.getTaxId() != null ? settlement.getTaxId() : "");
      context.setVariable(
          "buyerAddress",
          settlement.getCompanyAddress() != null ? settlement.getCompanyAddress() : "");

      context.setVariable("guestEmail", reservation.getGuestEmail());
      context.setVariable("unitName", reservation.getUnitName());
      context.setVariable("startDate", reservation.getStartDate());
      context.setVariable("endDate", reservation.getEndDate());

      String currency =
          (reservation.getCurrencyInfo() != null
                  && reservation.getCurrencyInfo().displayCurrency() != null)
              ? reservation.getCurrencyInfo().displayCurrency()
              : "PLN";
      context.setVariable("currency", currency);

      BigDecimal rate =
          (reservation.getCurrencyInfo() != null
                  && reservation.getCurrencyInfo().exchangeRate() != null)
              ? reservation.getCurrencyInfo().exchangeRate()
              : BigDecimal.ONE;

      String stripeSessionId = "";
      List<PaymentTransaction> txs =
          paymentTransactionRepository.findBySettlementId(settlement.getId());
      if (!txs.isEmpty()) {
        stripeSessionId = txs.get(0).getStripeSessionId();
      }
      context.setVariable("stripeReference", stripeSessionId);

      context.setVariable("settlement", settlement);
      context.setVariable(
          "formattedAccommodation",
          formatPrice(settlement.getAccommodationAmount(), currency, rate));
      context.setVariable(
          "formattedUtilities", formatPrice(settlement.getUtilitiesAmount(), currency, rate));
      context.setVariable(
          "formattedDeposit", formatPrice(settlement.getDepositAmount(), currency, rate));
      context.setVariable(
          "formattedDiscount", formatPrice(settlement.getDiscountAmount(), currency, rate));
      context.setVariable(
          "formattedTotal", formatPrice(settlement.getTotalAmount(), currency, rate));
      context.setVariable("formattedPaid", formatPrice(settlement.getAmountPaid(), currency, rate));
      context.setVariable(
          "formattedBalanceDue", formatPrice(settlement.getBalanceDue(), currency, rate));

      context.setVariable("statusLabel", settlement.getStatus().name());
      context.setVariable(
          "statusStyle",
          settlement.getStatus() == SettlementStatus.PAID
              ? "background-color: #D1FAE5; color: #065F46;"
              : "background-color: #FEF3C7; color: #92400E;");

      String htmlContent = templateEngine.process("settlement-invoice", context);

      java.nio.file.Path targetPath =
          java.nio.file.Paths.get("storage", "invoices", settlement.getId().toString() + ".pdf")
              .toAbsolutePath()
              .normalize();
      java.nio.file.Files.createDirectories(targetPath.getParent());

      try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(targetPath)) {
        com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
            new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(htmlContent, null);
        builder.toStream(os);
        builder.run();
      }

      settlement.setInvoicePdfPath(targetPath.toString());
      settlementRepository.save(settlement);

    } catch (Exception e) {
      log.error("Failed to generate PDF invoice for settlement: {}", settlementId, e);
    }
  }

  private String formatPrice(BigDecimal amount, String currency, BigDecimal rate) {
    if (amount == null) return "0.00 PLN";
    String formatted = String.format(java.util.Locale.US, "%.2f PLN", amount);
    if (currency != null
        && !"PLN".equalsIgnoreCase(currency)
        && rate != null
        && rate.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal converted = amount.divide(rate, 2, java.math.RoundingMode.HALF_UP);
      formatted +=
          String.format(
              java.util.Locale.US,
              " (%.2f %s)",
              converted,
              currency.toUpperCase(java.util.Locale.ROOT));
    }
    return formatted;
  }
}
