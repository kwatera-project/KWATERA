package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationStatusValidator {
    public void validateTransition(ReservationStatus currentStatus, ReservationStatus newStatus) {
        if (currentStatus == null || newStatus == null) throw new IllegalArgumentException("Status cannot be null.");
        if (currentStatus == newStatus) throw new IllegalArgumentException("Reservation already has the status: " + newStatus);

        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == ReservationStatus.CONFIRMED || newStatus == ReservationStatus.CANCELLED;
            case CONFIRMED -> newStatus == ReservationStatus.COMPLETED || newStatus == ReservationStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!isValid) throw new IllegalStateException("Illegal transition from: " + currentStatus + " to: " + newStatus);
    }
}