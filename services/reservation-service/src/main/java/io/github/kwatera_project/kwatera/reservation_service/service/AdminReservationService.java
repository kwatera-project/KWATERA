package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReservationService {

    private final ReservationRepository reservationRepository;

    public List<ReservationOverviewDto> getReservationsOverview(ReservationStatus status) {
        return List.of(
                new ReservationOverviewDto(
                        UUID.randomUUID(), "Testowy Admin", "Apartament 1",
                        LocalDate.now(), LocalDate.now().plusDays(2), ReservationStatus.CONFIRMED
                )
        );
    }

    private ReservationOverviewDto mapToOverviewDto(Reservation reservation) {
        String mockGuestName = "Guest (" + splitUuid(reservation.getUserId()) + ")";
        String mockUnitName = "Room (" + splitUuid(reservation.getUnitId()) + ")";

        return new ReservationOverviewDto(
                reservation.getId(), mockGuestName, mockUnitName,
                reservation.getStartDate(), reservation.getEndDate(), reservation.getStatus()
        );
    }

    private String splitUuid(UUID uuid) {
        return (uuid == null) ? "Blank" : uuid.toString().substring(0, 8);
    }
}