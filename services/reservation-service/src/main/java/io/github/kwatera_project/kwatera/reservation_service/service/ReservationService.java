package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.*;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.math.BigDecimal;
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

  public AvailabilityDto checkAvailability(UUID unitId, LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates are required");
    }
    if (from.isBefore(LocalDate.now())) {
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

      if (response == null || response.getBody() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found");
      }

      return response.getBody().getPricePerNight();

    } catch (HttpClientErrorException.NotFound e) {
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

    return reservationRepository.save(reservation);
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

    ReservationDetailsDto dto = new ReservationDetailsDto();
    dto.setId(reservation.getId());
    dto.setUserId(reservation.getUserId());
    dto.setUnitId(reservation.getUnitId());
    dto.setStartDate(reservation.getStartDate());
    dto.setEndDate(reservation.getEndDate());
    dto.setStatus(reservation.getStatus());
    dto.setCreatedAt(reservation.getCreatedAt());
    dto.setPricePerNightSnapshot(reservation.getPricePerNightSnapshot());
    dto.setTotalPrice(reservation.getTotalPrice());
    return dto;
  }

  public List<GuestReservationDto> getMyReservations(UUID userId) {
    return reservationRepository.findByUserId(userId).stream()
        .map(
            r ->
                new GuestReservationDto(
                    r.getId(), r.getUnitId(), r.getStartDate(), r.getEndDate(), r.getStatus()))
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
  }
}
