package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.dto.*;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

  private final ReservationRepository reservationRepository;
  private final RestTemplate restTemplate;
  private final NbpExchangeRateClient nbpExchangeRateClient;
  private final EmailNotificationService emailNotificationService;
  private final BusinessDateProvider businessDateProvider;

  public AvailabilityDto checkAvailability(UUID unitId, LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates are required");
    }

    LocalDate today = businessDateProvider.today();

    if (from.isBefore(today)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is in the past");
    }
    if (!from.isBefore(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
    }
    if (unitId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit id is required");
    }
    List<Reservation> reservations = reservationRepository.findByUnitId(unitId);
    for (Reservation r : reservations) {
      if (r.getStatus() == ReservationStatus.CANCELLED
          || r.getStatus() == ReservationStatus.COMPLETED) {
        continue;
      }
      if (from.isBefore(r.getEndDate()) && to.isAfter(r.getStartDate())) {
        return new AvailabilityDto(false, "Unit is not available in selected dates");
      }
    }
    return new AvailabilityDto(true, "Unit is available");
  }

  public List<DateRangeDto> getOccupiedDates(UUID unitId) {
    return reservationRepository.findByUnitId(unitId).stream()
        .filter(
            r ->
                r.getStatus() != ReservationStatus.CANCELLED
                    && r.getStatus() != ReservationStatus.COMPLETED)
        .map(r -> new DateRangeDto(r.getStartDate(), r.getEndDate()))
        .toList();
  }

  public List<OccupancyDto> getOccupancy(
      LocalDate start, LocalDate end, UUID ownerId, boolean isAdmin) {

    List<UUID> allUnitIds = fetchAllRelevantUnitIds(ownerId, isAdmin);
    if (allUnitIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }

    java.util.Map<UUID, String> unitNames = new java.util.HashMap<>();
    for (UUID unitId : allUnitIds) {
      String name = "Room " + unitId.toString().substring(0, 8);
      try {
        String unitUrl = "http://property-service/api/properties/units/" + unitId;
        UnitDto unitDto = restTemplate.getForObject(unitUrl, UnitDto.class);
        if (unitDto != null && unitDto.getName() != null) {
          name = unitDto.getName();
        }
      } catch (Exception e) {
        log.warn("Failed to fetch unit name for unit {}: {}", unitId, e.getMessage());
      }
      unitNames.put(unitId, name);
    }

    List<Reservation> reservations =
        reservationRepository.findByUnitIdIn(allUnitIds).stream()
            .filter(r -> !r.getEndDate().isBefore(start) && !r.getStartDate().isAfter(end))
            .toList();

    java.util.Set<UUID> unitsWithReservations = new java.util.HashSet<>();
    List<OccupancyDto> result = new java.util.ArrayList<>();

    for (Reservation r : reservations) {
      unitsWithReservations.add(r.getUnitId());
      result.add(
          new OccupancyDto(
              r.getId(),
              r.getUnitId(),
              unitNames.getOrDefault(
                  r.getUnitId(), "Room " + r.getUnitId().toString().substring(0, 8)),
              r.getStartDate(),
              r.getEndDate(),
              r.getStatus().name()));
    }

    for (UUID unitId : allUnitIds) {
      if (!unitsWithReservations.contains(unitId)) {
        result.add(
            new OccupancyDto(
                null,
                unitId,
                unitNames.getOrDefault(unitId, "Room " + unitId.toString().substring(0, 8)),
                null,
                null,
                "FREE"));
      }
    }

    return result;
  }

  private List<UUID> fetchAllRelevantUnitIds(UUID ownerId, boolean isAdmin) {
    try {
      String url =
          isAdmin
              ? "http://property-service/api/properties/units/ids"
              : "http://property-service/api/properties/units/ids/" + ownerId;
      UUID[] unitIdsArray = restTemplate.getForObject(url, UUID[].class);
      return unitIdsArray != null ? Arrays.asList(unitIdsArray) : java.util.Collections.emptyList();
    } catch (Exception e) {
      log.warn("Error fetching unit IDs from property-service: {}", e.getMessage());
      return java.util.Collections.emptyList();
    }
  }

  private BigDecimal fetchUnitPrice(UUID unitId, String token) {
    String url = "http://property-service/api/properties/units/{unitId}";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<UnitDto> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, UnitDto.class, unitId);

      if (response.getBody() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found");
      }

      return response.getBody().getPricePerNight();

    } catch (HttpClientErrorException.NotFound _) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found");

    } catch (ResponseStatusException e) {
      throw e;

    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Cannot fetch unit price: " + e.getMessage());
    }
  }

  @Transactional
  public Reservation createReservation(
      UUID userId, CreateReservationRequest request, String token) {
    return createReservationInternal(userId, null, request, token);
  }

  @Transactional
  public Reservation createReservation(
      UUID userId, String guestEmail, CreateReservationRequest request, String token) {
    return createReservationInternal(userId, guestEmail, request, token);
  }

  private Reservation createReservationInternal(
      UUID userId, String guestEmail, CreateReservationRequest request, String token) {
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
    }
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation request is required");
    }
    AvailabilityDto availability =
        checkAvailability(request.getUnitId(), request.getStartDate(), request.getEndDate());
    if (!availability.isAvailable()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "The selected dates are no longer available");
    }

    Reservation reservation = new Reservation();
    reservation.setUserId(userId);
    reservation.setGuestEmail(guestEmail);
    reservation.setUnitId(request.getUnitId());
    reservation.setStartDate(request.getStartDate());
    reservation.setEndDate(request.getEndDate());
    reservation.setStatus(ReservationStatus.PENDING);

    BigDecimal pricePerNight = fetchUnitPrice(request.getUnitId(), token);
    long days =
        java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
    BigDecimal totalPrice = pricePerNight.multiply(BigDecimal.valueOf(days));

    reservation.setPricePerNightSnapshot(pricePerNight);
    reservation.setTotalPrice(totalPrice);

    String paymentCurrency = "PLN";
    BigDecimal paymentExchangeRate = BigDecimal.ONE;

    if (request.getCurrency() != null && !"PLN".equalsIgnoreCase(request.getCurrency())) {
      String requestedCurrency = request.getCurrency().toUpperCase(java.util.Locale.ROOT);

      try {
        NbpResponseDto nbpResponse =
            switch (requestedCurrency) {
              case "EUR" -> nbpExchangeRateClient.getEurExchangeRate();
              case "USD" -> nbpExchangeRateClient.getUsdExchangeRate();
              default ->
                  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported currency");
            };

        if (nbpResponse != null && nbpResponse.rates() != null && !nbpResponse.rates().isEmpty()) {
          paymentCurrency = requestedCurrency;
          paymentExchangeRate = nbpResponse.rates().get(0).mid();
        }
      } catch (ResponseStatusException e) {
        throw e;
      } catch (Exception e) {
        log.warn("Failed to fetch exchange rate from NBP; falling back to PLN");
      }
    }
    reservation.setPaymentCurrency(paymentCurrency);
    reservation.setPaymentExchangeRate(paymentExchangeRate);

    Reservation saved = reservationRepository.save(reservation);
    emailNotificationService.sendReservationCreated(saved, saved.getGuestEmail());
    try {
      emailNotificationService.sendOwnerReservationCreated(saved);
    } catch (Exception e) {
      log.warn("Failed to send owner notification for reservation creation: {}", e.getMessage());
    }
    return saved;
  }

  public ReservationDetailsDto getReservationDetails(
      UUID reservationId, UUID userId, boolean isAdmin, boolean isOwner) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

    boolean isGuestOwner = reservation.getUserId().equals(userId);
    boolean isPropertyOwner = isOwner && ownerHasAccessToUnit(userId, reservation.getUnitId());

    if (!isAdmin && !isGuestOwner && !isPropertyOwner) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    return mapAndEnrichReservationDetails(reservation);
  }

  public ReservationDetailsDto getReservationDetailsInternal(UUID reservationId) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

    return mapAndEnrichReservationDetails(reservation);
  }

  private ReservationDetailsDto mapAndEnrichReservationDetails(Reservation reservation) {
    ReservationDetailsDto dto = new ReservationDetailsDto();
    dto.setId(reservation.getId());
    dto.setUserId(reservation.getUserId());
    dto.setGuestEmail(reservation.getGuestEmail());
    dto.setUnitId(reservation.getUnitId());
    dto.setStartDate(reservation.getStartDate());
    dto.setEndDate(reservation.getEndDate());
    dto.setStatus(reservation.getStatus());
    dto.setCreatedAt(reservation.getCreatedAt());
    dto.setPricePerNightSnapshot(reservation.getPricePerNightSnapshot());
    dto.setTotalPrice(reservation.getTotalPrice());

    CurrencyMetadataDto currencyInfo =
        createCurrencyInfo(
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate(),
            reservation.getCreatedAt());
    BigDecimal convertedTotalPrice =
        calculateConvertedTotalPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate());

    dto.setConvertedTotalPrice(convertedTotalPrice);
    dto.setCurrencyInfo(currencyInfo);
    String guestName = "Guest " + reservation.getUserId().toString().substring(0, 8);
    String unitName = "Unknown Room";
    String city = "Unknown City";

    try {
      String unitUrl = "http://property-service/api/properties/units/" + reservation.getUnitId();
      UnitDetailsDto unitDto = restTemplate.getForObject(unitUrl, UnitDetailsDto.class);
      if (unitDto != null) {
        if (unitDto.name() != null) {
          unitName = unitDto.name();
        }
        if (unitDto.propertyId() != null) {
          String propertyUrl = "http://property-service/api/properties/" + unitDto.propertyId();
          PropertyDetailsDto propertyDto =
              restTemplate.getForObject(propertyUrl, PropertyDetailsDto.class);
          if (propertyDto != null) {
            if (propertyDto.city() != null) {
              city = propertyDto.city();
            }
            if (propertyDto.ownerId() != null) {
              UUID ownerId = propertyDto.ownerId();
              String ownerName = "Owner " + ownerId.toString().substring(0, 8);
              String ownerEmail = "owner_" + ownerId.toString().substring(0, 8) + "@example.com";
              if (ownerId.toString().equals("22222222-2222-2222-2222-222222222222")) {
                ownerName = "John Owner";
                ownerEmail = "owner1@example.com";
              } else if (ownerId.toString().equals("33333333-3333-3333-3333-333333333333")) {
                ownerName = "Jane Owner";
                ownerEmail = "owner2@example.com";
              }
              dto.setOwnerName(ownerName);
              dto.setOwnerEmail(ownerEmail);
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to fetch property details for reservation: {}", reservation.getId(), e);
    }

    dto.setGuestName(guestName);
    dto.setUnitName(unitName);
    dto.setCity(city);

    return dto;
  }

  public List<GuestReservationDto> getMyReservations(UUID userId) {
    return reservationRepository.findByUserId(userId).stream()
        .map(
            r -> {
              CurrencyMetadataDto currencyInfo =
                  createCurrencyInfo(
                      r.getPaymentCurrency(), r.getPaymentExchangeRate(), r.getCreatedAt());
              BigDecimal convertedTotalPrice =
                  calculateConvertedTotalPrice(
                      r.getTotalPrice(), r.getPaymentCurrency(), r.getPaymentExchangeRate());

              return new GuestReservationDto(
                  r.getId(),
                  r.getUnitId(),
                  r.getStartDate(),
                  r.getEndDate(),
                  r.getStatus(),
                  r.getTotalPrice(),
                  convertedTotalPrice,
                  currencyInfo);
            })
        .toList();
  }

  private boolean ownerHasAccessToUnit(UUID ownerId, UUID unitId) {
    String propertyServiceUrl = "http://property-service/api/properties/units/ids/{ownerId}";
    try {
      UUID[] unitIdsArray = restTemplate.getForObject(propertyServiceUrl, UUID[].class, ownerId);
      if (unitIdsArray == null) {
        return false;
      }
      return Arrays.asList(unitIdsArray).contains(unitId);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Unable to verify ownership: " + e.getMessage());
    }
  }

  @Transactional
  public void handleSettlementStatusUpdate(UUID reservationId, SettlementStatus settlementStatus) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
    ReservationStatus oldStatus = reservation.getStatus();

    switch (settlementStatus) {
      case PAID -> reservation.setStatus(ReservationStatus.COMPLETED);

      case PARTIALLY_PAID -> reservation.setStatus(ReservationStatus.CONFIRMED);

      case CANCELLED -> reservation.setStatus(ReservationStatus.CANCELLED);

      case ISSUED, DRAFT -> {
        return;
      }

      default -> throw new IllegalStateException("Unhandled settlementStatus: " + settlementStatus);
    }

    reservationRepository.save(reservation);
    emailNotificationService.sendReservationStatusChanged(
        reservation, oldStatus, reservation.getStatus(), reservation.getGuestEmail());
    try {
      if (reservation.getStatus() == ReservationStatus.CANCELLED) {
        emailNotificationService.sendOwnerReservationCancelled(reservation);
      } else {
        emailNotificationService.sendOwnerReservationStatusChanged(
            reservation, oldStatus, reservation.getStatus());
      }
    } catch (Exception e) {
      log.warn(
          "Failed to send owner notification for reservation status update: {}", e.getMessage());
    }
  }

  public ReservationMetricsDto getDashboardReservationMetrics(
      LocalDate startDate, LocalDate endDate, UUID ownerId, boolean isAdmin) {

    LocalDate today = businessDateProvider.today();

    LocalDate start = (startDate != null) ? startDate : today.withDayOfMonth(1);
    LocalDate end =
        (endDate != null)
            ? endDate
            : today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

    if (start.isAfter(end)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
    }

    List<UUID> allUnitIds = fetchAllRelevantUnitIds(ownerId, isAdmin);
    if (allUnitIds.isEmpty()) {
      return new ReservationMetricsDto(0L, 0.0, 0L);
    }

    long totalReservations =
        isAdmin
            ? reservationRepository.countReservationsInDateRange(start, end)
            : reservationRepository.countReservationsInDateRangeForUnits(allUnitIds, start, end);

    List<Reservation> reservations =
        isAdmin
            ? reservationRepository.findActiveReservationsInDateRange(start, end)
            : reservationRepository.findActiveReservationsInDateRangeForUnits(
                allUnitIds, start, end);

    long totalDaysInRange = java.time.temporal.ChronoUnit.DAYS.between(start, end);
    if (totalDaysInRange <= 0) {
      totalDaysInRange = 1;
    }

    long totalAvailableNights = allUnitIds.size() * totalDaysInRange;

    long occupiedNights = 0;
    for (Reservation r : reservations) {
      LocalDate overlapStart = r.getStartDate().isBefore(start) ? start : r.getStartDate();
      LocalDate overlapEnd = r.getEndDate().isAfter(end) ? end : r.getEndDate();

      if (overlapStart.isBefore(overlapEnd)) {
        occupiedNights += java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd);
      }
    }

    double occupancyRate = 0.0;
    if (totalAvailableNights > 0) {
      occupancyRate = ((double) occupiedNights / totalAvailableNights) * 100.0;
      occupancyRate = Math.round(occupancyRate * 100.0) / 100.0;
    }

    if (occupancyRate > 100.0) {
      occupancyRate = 100.0;
    }

    return new ReservationMetricsDto(totalReservations, occupancyRate, occupiedNights);
  }

  @Transactional
  public void cancelExpiredPendingReservations(java.time.Instant threshold) {
    log.info("Starting cleanup of expired pending reservations older than: {}", threshold);
    List<Reservation> expiredReservations =
        reservationRepository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, threshold);

    for (Reservation reservation : expiredReservations) {
      ReservationStatus oldStatus = reservation.getStatus();
      reservation.setStatus(ReservationStatus.CANCELLED);
      reservationRepository.save(reservation);
      log.info("Cancelled expired pending reservation with ID: {}", reservation.getId());

      try {
        emailNotificationService.sendReservationStatusChanged(
            reservation, oldStatus, ReservationStatus.CANCELLED, reservation.getGuestEmail());
        emailNotificationService.sendOwnerReservationCancelled(reservation);
      } catch (Exception e) {
        log.warn(
            "Failed to send cancellation email for reservation {}: {}",
            reservation.getId(),
            e.getMessage());
      }
    }
  }

  public void notifyUpcomingReservations() {
    LocalDate tomorrow = businessDateProvider.today().plusDays(1);
    List<Reservation> upcoming =
        reservationRepository.findByStartDateAndStatus(tomorrow, ReservationStatus.CONFIRMED);
    for (Reservation reservation : upcoming) {
      try {
        emailNotificationService.sendOwnerReservationUpcoming(reservation);
      } catch (Exception e) {
        log.warn(
            "Failed to send upcoming reservation notification to owner for reservation {}: {}",
            reservation.getId(),
            e.getMessage());
      }
    }
  }

  private CurrencyMetadataDto createCurrencyInfo(
      String currency, BigDecimal exchangeRate, java.time.Instant createdAt) {
    return new CurrencyMetadataDto(
        "PLN",
        currency,
        exchangeRate,
        createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
  }

  private BigDecimal calculateConvertedTotalPrice(
      BigDecimal totalPrice, String currency, BigDecimal exchangeRate) {
    if (!"PLN".equalsIgnoreCase(currency)) {
      return totalPrice.divide(exchangeRate, 2, RoundingMode.HALF_UP);
    }
    return totalPrice;
  }

  record UnitDetailsDto(UUID propertyId, String name) {}

  record PropertyDetailsDto(String title, String city, UUID ownerId) {}
}
