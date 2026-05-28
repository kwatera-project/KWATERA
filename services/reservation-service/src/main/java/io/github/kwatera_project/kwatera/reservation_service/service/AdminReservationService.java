package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatusHistory;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminReservationService {

  private final ReservationRepository reservationRepository;

  private final ReservationStatusHistoryRepository statusHistoryRepository;

  private final ReservationStatusValidator statusValidator;

  private final RestTemplate restTemplate;

  private final EmailNotificationService emailNotificationService;

  public AdminReservationService(
      ReservationRepository reservationRepository,
      ReservationStatusHistoryRepository statusHistoryRepository,
      ReservationStatusValidator statusValidator,
      RestTemplate restTemplate) {
    this(reservationRepository, statusHistoryRepository, statusValidator, restTemplate, null);
  }

  @Autowired
  public AdminReservationService(
      ReservationRepository reservationRepository,
      ReservationStatusHistoryRepository statusHistoryRepository,
      ReservationStatusValidator statusValidator,
      RestTemplate restTemplate,
      EmailNotificationService emailNotificationService) {
    this.reservationRepository = reservationRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.statusValidator = statusValidator;
    this.restTemplate = restTemplate;
    this.emailNotificationService = emailNotificationService;
  }

  public List<ReservationOverviewDto> getReservationsOverview(
      UUID ownerId, ReservationStatus status, boolean isAdmin) {

    if (isAdmin) {
      List<Reservation> reservations =
          (status != null)
              ? reservationRepository.findByStatus(status)
              : reservationRepository.findAll();

      return reservations.stream().map(this::mapToOverviewDto).toList();
    }

    String propertyServiceUrl = "http://property-service/api/properties/units/ids/" + ownerId;

    try {
      UUID[] unitIdsArray = restTemplate.getForObject(propertyServiceUrl, UUID[].class);
      List<UUID> ownerUnitIds =
          unitIdsArray != null ? Arrays.asList(unitIdsArray) : Collections.emptyList();

      if (ownerUnitIds.isEmpty()) {
        return Collections.emptyList();
      }

      List<Reservation> reservations =
          (status != null)
              ? reservationRepository.findByUnitIdInAndStatus(ownerUnitIds, status)
              : reservationRepository.findByUnitIdIn(ownerUnitIds);

      return reservations.stream().map(this::mapToOverviewDto).toList();

    } catch (Exception e) {
      System.err.println("Error connection with property-service: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  public ReservationOverviewDto updateReservationStatus(
      UUID reservationId, ReservationStatus newStatus, UUID userId, boolean isAdmin) {

    if (newStatus == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New status cannot be null");
    }

    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

    ReservationStatus oldStatus = reservation.getStatus();

    if (!isAdmin) {
      verifyOwnerAccess(userId, reservation.getUnitId());
    }

    try {
      statusValidator.validateTransition(oldStatus, newStatus);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    reservation.setStatus(newStatus);
    reservationRepository.save(reservation);

    ReservationStatusHistory history = new ReservationStatusHistory();
    history.setReservationId(reservationId);
    history.setOldStatus(oldStatus);
    history.setNewStatus(newStatus);
    history.setChangedBy(userId);
    history.setChangedAt(LocalDateTime.now());
    statusHistoryRepository.save(history);

    if (emailNotificationService != null) {
      emailNotificationService.sendReservationStatusChanged(
          reservation, oldStatus, newStatus, reservation.getGuestEmail());
    }

    return mapToOverviewDto(reservation);
  }

  private void verifyOwnerAccess(UUID ownerId, UUID unitId) {
    String propertyServiceUrl = "http://property-service/api/properties/units/ids/" + ownerId;
    try {
      UUID[] unitIdsArray = restTemplate.getForObject(propertyServiceUrl, UUID[].class);
      List<UUID> ownerUnitIds =
          unitIdsArray != null ? Arrays.asList(unitIdsArray) : Collections.emptyList();

      if (!ownerUnitIds.contains(unitId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You are not allowed to update this reservation");
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Unable to verify ownership: " + e.getMessage());
    }
  }

  private ReservationOverviewDto mapToOverviewDto(Reservation reservation) {
    String guestName = "Guest " + splitUuid(reservation.getUserId());
    String unitName = "Unknown Room";

    try {
      String unitUrl = "http://property-service/api/properties/units/" + reservation.getUnitId();
      UnitNameDto unitDto = restTemplate.getForObject(unitUrl, UnitNameDto.class);
      if (unitDto != null && unitDto.name() != null) {
        unitName = unitDto.name();
      } else {
        unitName = "Room " + splitUuid(reservation.getUnitId());
      }
    } catch (Exception e) {
      unitName = "Room " + splitUuid(reservation.getUnitId());
    }

    return new ReservationOverviewDto(
        reservation.getId(),
        guestName,
        unitName,
        reservation.getStartDate(),
        reservation.getEndDate(),
        reservation.getStatus(),
        reservation.getPricePerNightSnapshot(),
        reservation.getTotalPrice());
  }

  private String splitUuid(UUID uuid) {
    return (uuid == null) ? "Blank" : uuid.toString().substring(0, 8);
  }

  private record UnitNameDto(String name) {}
}
