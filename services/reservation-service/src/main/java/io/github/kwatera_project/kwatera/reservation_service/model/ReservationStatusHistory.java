package io.github.kwatera_project.kwatera.reservation_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_status_history")
public class ReservationStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "reservation_id", nullable = false)
  private UUID reservationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status")
  private ReservationStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false)
  private ReservationStatus newStatus;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt = LocalDateTime.now();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID reservationId) {
    this.reservationId = reservationId;
  }

  public ReservationStatus getOldStatus() {
    return oldStatus;
  }

  public void setOldStatus(ReservationStatus oldStatus) {
    this.oldStatus = oldStatus;
  }

  public ReservationStatus getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(ReservationStatus newStatus) {
    this.newStatus = newStatus;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(UUID changedBy) {
    this.changedBy = changedBy;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(LocalDateTime changedAt) {
    this.changedAt = changedAt;
  }
}
