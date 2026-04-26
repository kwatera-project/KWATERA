package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {

  private final ReservationRepository reservationRepository;

  public ReservationService(ReservationRepository reservationRepository) {
    this.reservationRepository = reservationRepository;
  }

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

  @Transactional
  public Reservation createReservation(UUID userId, CreateReservationRequest request) {
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

    return reservationRepository.save(reservation);
  }
}
