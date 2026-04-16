package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReservationStatusValidatorTest {

    private final ReservationStatusValidator validator = new ReservationStatusValidator();

    @Test
    void shouldAllowTransitionFromPendingToConfirmed() {
        assertDoesNotThrow(() ->
                validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );
    }

    @Test
    void shouldAllowTransitionFromPendingToCancelled() {
        assertDoesNotThrow(() ->
                validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.CANCELLED)
        );
    }

    @Test
    void shouldAllowTransitionFromConfirmedToCompleted() {
        assertDoesNotThrow(() ->
                validator.validateTransition(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED)
        );
    }

    @Test
    void shouldBlockTransitionFromCompletedToPending() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                validator.validateTransition(ReservationStatus.COMPLETED, ReservationStatus.PENDING)
        );
        assertTrue(exception.getMessage().contains("Illegal transition"));
    }

    @Test
    void shouldThrowExceptionWhenStatusIsTheSame() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.PENDING)
        );
    }
}