Feature: Reservation flow

  Scenario: Guest creates a reservation for an available unit
    Given a unit is available for a selected future date range
    When the guest creates a reservation for that date range
    Then the reservation should be created with PENDING status
    And the reservation should be persisted with the correct unit, guest, dates, price snapshot, and total price

  Scenario: Overlapping reservation is rejected
    Given a guest has already created a reservation for a unit and date range
    When the guest tries to create another reservation for the same unit with overlapping dates
    Then the second reservation should be rejected with conflict
    And only the original reservation should remain persisted
