package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AdminReservationService {

  private final ReservationRepository reservationRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public List<ReservationOverviewDto> getReservationsOverview(
      UUID ownerId, ReservationStatus status, boolean isAdmin) {

    if (isAdmin) {
      List<Reservation> reservations =
          (status != null)
              ? reservationRepository.findByStatus(status)
              : reservationRepository.findAll();

      return reservations.stream().map(this::mapToOverviewDto).toList();
    }

    String propertyServiceUrl = "http://property-service:8083/api/properties/units/ids/" + ownerId;

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

  private ReservationOverviewDto mapToOverviewDto(Reservation reservation) {
    String guestName = "Guest " + splitUuid(reservation.getUserId());
    String unitName = "Unknown Room";

    try {
      String unitUrl =
          "http://property-service:8083/api/properties/units/" + reservation.getUnitId();
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
        reservation.getStatus());
  }

  private String splitUuid(UUID uuid) {
    return (uuid == null) ? "Blank" : uuid.toString().substring(0, 8);
  }

  private record UnitNameDto(String name) {}
}
