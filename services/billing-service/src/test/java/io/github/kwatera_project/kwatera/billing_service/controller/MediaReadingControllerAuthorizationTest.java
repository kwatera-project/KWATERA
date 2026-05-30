package io.github.kwatera_project.kwatera.billing_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class MediaReadingControllerAuthorizationTest {

  @Test
  void shouldRestrictInitialAndFinalUploadsToGuests() throws Exception {
    assertPreAuthorize("uploadInitialReading", "hasAuthority('ROLE_GUEST')");
    assertPreAuthorize("uploadFinalReading", "hasAuthority('ROLE_GUEST')");
  }

  @Test
  void shouldRestrictManualApprovalAndAttemptsToReviewers() throws Exception {
    assertPreAuthorize("manuallyApproveReading", "hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')");
    assertPreAuthorize("getUploadAttempts", "hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')");
  }

  @Test
  void shouldAllowReadEndpointForGuestsAndReviewers() throws Exception {
    assertPreAuthorize(
        "getMediaReadings", "hasAnyAuthority('ROLE_GUEST', 'ROLE_OWNER', 'ROLE_ADMIN')");
  }

  private void assertPreAuthorize(String methodName, String expectedExpression) {
    Method method =
        java.util.Arrays.stream(MediaReadingController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();

    assertEquals(expectedExpression, method.getAnnotation(PreAuthorize.class).value());
  }
}
