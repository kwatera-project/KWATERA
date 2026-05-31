Feature: Stage 3 billing flow

  Scenario: Guest reservation is settled after accepted OCR reading and payment
    Given a reservation exists with accommodation amount 500.0 PLN
    And the initial water meter reading was approved with value 100
    When the guest uploads a final water meter photo
    And the OCR reads the meter value as 150 with confidence 0.95
    Then the final reading status should be AUTO_APPROVED
    And a water utility charge of 250.0 PLN should be added to the settlement
    And the settlement total should be 750.0 PLN
    When the guest pays 500.0 PLN for accommodation
    Then the settlement amount paid should be 500.0 PLN
    And the settlement balance due should be 250.0 PLN
    And the settlement status should be ISSUED

  Scenario: Invalid OCR reading does not update settlement
    Given a reservation exists with accommodation amount 500.0 PLN
    And the initial water meter reading was approved with value 100
    When the guest uploads a final water meter photo
    And the OCR returns an invalid meter value
    Then the final reading status should be REQUEST_REUPLOAD
    And no utility charge should be added to the settlement
    And the settlement total should be 500.0 PLN
    And the settlement balance due should be 500.0 PLN

  Scenario: Duplicate water utility charge is rejected
    Given a reservation exists with accommodation amount 500.0 PLN
    And a water utility charge of 250.0 PLN has been added to the settlement
    When a duplicate water utility charge is submitted
    Then the system should reject it with an error