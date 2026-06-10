package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventType;
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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminReservationService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(AdminReservationService.class);

  private final ReservationRepository reservationRepository;

  private final ReservationStatusHistoryRepository statusHistoryRepository;

  private final ReservationStatusValidator statusValidator;

  private final ObjectFactory<RestOperations> restOperationsFactory;

  private final EmailNotificationService emailNotificationService;

  private final SystemEventService systemEventService;

  @Autowired
  public AdminReservationService(
      ReservationRepository reservationRepository,
      ReservationStatusHistoryRepository statusHistoryRepository,
      ReservationStatusValidator statusValidator,
      ObjectFactory<RestOperations> restOperationsFactory,
      EmailNotificationService emailNotificationService,
      SystemEventService systemEventService) {
    this.reservationRepository = reservationRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.statusValidator = statusValidator;
    this.restOperationsFactory = restOperationsFactory;
    this.emailNotificationService = emailNotificationService;
    this.systemEventService = systemEventService;
  }

  public AdminReservationService(
      ReservationRepository reservationRepository,
      ReservationStatusHistoryRepository statusHistoryRepository,
      ReservationStatusValidator statusValidator,
      RestOperations restTemplate,
      EmailNotificationService emailNotificationService) {
    this(
        reservationRepository,
        statusHistoryRepository,
        statusValidator,
        () -> restTemplate,
        emailNotificationService,
        null);
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
      UUID[] unitIdsArray = restOperations().getForObject(propertyServiceUrl, UUID[].class);
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
      log.error("Error connection with property-service", e);
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

    logStatusChange(userId, reservation, oldStatus, newStatus);

    emailNotificationService.sendReservationStatusChanged(
        reservation, oldStatus, newStatus, reservation.getGuestEmail());

    try {
      if (newStatus == ReservationStatus.CANCELLED) {
        emailNotificationService.sendOwnerReservationCancelled(reservation);
      } else {
        emailNotificationService.sendOwnerReservationStatusChanged(
            reservation, oldStatus, newStatus);
      }
    } catch (Exception e) {
      log.warn("Failed to send owner notification for reservation status update", e);
    }

    return mapToOverviewDto(reservation);
  }

  private void verifyOwnerAccess(UUID ownerId, UUID unitId) {
    String propertyServiceUrl = "http://property-service/api/properties/units/ids/" + ownerId;
    try {
      UUID[] unitIdsArray = restOperations().getForObject(propertyServiceUrl, UUID[].class);
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
      UnitNameDto unitDto = restOperations().getForObject(unitUrl, UnitNameDto.class);
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

  private RestOperations restOperations() {
    return restOperationsFactory.getObject();
  }

  public boolean hasReservationsForUnit(UUID unitId) {
    return reservationRepository.existsByUnitIdAndStatusIn(
        unitId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
  }

  private void logStatusChange(
      UUID actorId,
      Reservation reservation,
      ReservationStatus oldStatus,
      ReservationStatus newStatus) {
    if (systemEventService == null) {
      return;
    }
    systemEventService.logSafely(
        SystemEventType.RESERVATION_STATUS_CHANGED,
        actorId,
        SystemEventService.ENTITY_TYPE_RESERVATION,
        reservation.getId(),
        "reservationId="
            + reservation.getId()
            + ", unitId="
            + reservation.getUnitId()
            + ", startDate="
            + reservation.getStartDate()
            + ", endDate="
            + reservation.getEndDate()
            + ", oldStatus="
            + oldStatus
            + ", newStatus="
            + newStatus);
  }
}
