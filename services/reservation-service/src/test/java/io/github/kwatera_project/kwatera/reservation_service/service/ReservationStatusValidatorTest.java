package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReservationStatusValidatorTest {

  private final ReservationStatusValidator validator = new ReservationStatusValidator();

  @Test
  void shouldAllowTransitionFromPendingToConfirmed() {
    assertDoesNotThrow(
        () -> validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
  }

  @Test
  void shouldAllowTransitionFromPendingToCancelled() {
    assertDoesNotThrow(
        () -> validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.CANCELLED));
  }

  @Test
  void shouldAllowTransitionFromConfirmedToCompleted() {
    assertDoesNotThrow(
        () ->
            validator.validateTransition(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED));
  }

  @Test
  void shouldAllowTransitionFromConfirmedToCancelled() {
    assertDoesNotThrow(
        () ->
            validator.validateTransition(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED));
  }

  @Test
  void shouldBlockTransitionFromPendingToCompleted() {
    assertThrows(
        IllegalStateException.class,
        () -> validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.COMPLETED));
  }

  @Test
  void shouldBlockTransitionFromConfirmedToPending() {
    assertThrows(
        IllegalStateException.class,
        () -> validator.validateTransition(ReservationStatus.CONFIRMED, ReservationStatus.PENDING));
  }

  @Test
  void shouldBlockTransitionFromCompletedToPending() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                validator.validateTransition(
                    ReservationStatus.COMPLETED, ReservationStatus.PENDING));
    assertTrue(exception.getMessage().contains("Illegal transition"));
  }

  @Test
  void shouldBlockTransitionFromCancelledToConfirmed() {
    assertThrows(
        IllegalStateException.class,
        () ->
            validator.validateTransition(ReservationStatus.CANCELLED, ReservationStatus.CONFIRMED));
  }

  @Test
  void shouldThrowExceptionWhenStatusIsTheSame() {
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateTransition(ReservationStatus.PENDING, ReservationStatus.PENDING));
  }

  @Test
  void shouldThrowExceptionWhenCurrentStatusIsNull() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateTransition(null, ReservationStatus.CONFIRMED));
    assertEquals("Status cannot be null.", ex.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenNewStatusIsNull() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateTransition(ReservationStatus.PENDING, null));
    assertEquals("Status cannot be null.", ex.getMessage());
  }

  @ParameterizedTest
  @EnumSource(ReservationStatus.class)
  void shouldBlockAllTransitionsFromCompleted(ReservationStatus target) {
    if (target == ReservationStatus.COMPLETED) return;
    assertThrows(
        IllegalStateException.class,
        () -> validator.validateTransition(ReservationStatus.COMPLETED, target));
  }

  @ParameterizedTest
  @EnumSource(ReservationStatus.class)
  void shouldBlockAllTransitionsFromCancelled(ReservationStatus target) {
    if (target == ReservationStatus.CANCELLED) return;
    assertThrows(
        IllegalStateException.class,
        () -> validator.validateTransition(ReservationStatus.CANCELLED, target));
  }
}
