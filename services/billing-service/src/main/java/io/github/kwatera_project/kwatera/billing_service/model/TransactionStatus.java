package io.github.kwatera_project.kwatera.billing_service.model;

public enum TransactionStatus {
  SUCCESS,
  FAILED,
  PROCESSING;

  public boolean isTerminal() {
    return this == SUCCESS || this == FAILED;
  }
}
