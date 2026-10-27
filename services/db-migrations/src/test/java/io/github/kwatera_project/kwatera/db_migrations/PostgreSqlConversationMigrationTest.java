package io.github.kwatera_project.kwatera.db_migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlConversationMigrationTest {

  private static final UUID EVIDENCED_CANCELLATION =
      UUID.fromString("91000000-0000-0000-0000-000000000001");
  private static final UUID UNKNOWN_CANCELLATION =
      UUID.fromString("91000000-0000-0000-0000-000000000002");
  private static final LocalDateTime FIRST_CANCELLATION = LocalDateTime.of(2026, 10, 20, 12, 30);

  @Container static final PostgreSQLContainer POSTGRES = postgresWithSeedImages();

  @Test
  void shouldMigrateRealPostgreSqlAndPreserveConversationConstraints() throws Exception {
    flyway(MigrationVersion.fromVersion("34")).migrate();
    insertLegacyReservationsAndEvents();

    var result = flyway(MigrationVersion.fromVersion("35")).migrate();

    assertThat(result.targetSchemaVersion).isEqualTo("35");
    assertThat(cancelledAt(EVIDENCED_CANCELLATION)).isEqualTo(FIRST_CANCELLATION);
    assertThat(cancelledAt(UNKNOWN_CANCELLATION)).isNull();

    UUID conversationId = UUID.randomUUID();
    insertConversation(conversationId, EVIDENCED_CANCELLATION);
    assertThatThrownBy(() -> insertConversation(UUID.randomUUID(), EVIDENCED_CANCELLATION))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> deleteReservation(EVIDENCED_CANCELLATION))
        .isInstanceOf(SQLException.class);

    assertThat(flyway(MigrationVersion.fromVersion("38")).migrate().targetSchemaVersion)
        .isEqualTo("38");
    try (var connection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      MessageMigrationTest.verifyConstraintsAndOrdering(connection, conversationId);
    }
  }

  private Flyway flyway(MigrationVersion target) {
    var configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  private static PostgreSQLContainer postgresWithSeedImages() {
    PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("kwatera_migration_test")
            .withUsername("kwatera")
            .withPassword("kwatera");
    postgres.withCopyToContainer(Transferable.of(new byte[] {0}), "/images/check_in.png");
    for (int number = 1; number <= 8; number++) {
      postgres.withCopyToContainer(
          Transferable.of(new byte[] {0}), "/images/check_out_" + number + ".png");
    }
    return postgres;
  }

  private void insertLegacyReservationsAndEvents() throws SQLException {
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement()) {
      statement.execute(
          legacyReservationInsert(EVIDENCED_CANCELLATION)
              + ";"
              + legacyReservationInsert(UNKNOWN_CANCELLATION));
      statement.execute(
          "INSERT INTO system_events "
              + "(id, timestamp, action_type, entity_type, entity_id, details) VALUES "
              + "(gen_random_uuid(), TIMESTAMP '2026-10-20 12:30:00', "
              + "'RESERVATION_STATUS_CHANGED', 'RESERVATION', '"
              + EVIDENCED_CANCELLATION
              + "', 'oldStatus=CONFIRMED, newStatus=CANCELLED'), "
              + "(gen_random_uuid(), TIMESTAMP '2026-10-21 12:30:00', "
              + "'RESERVATION_STATUS_CHANGED', 'RESERVATION', '"
              + EVIDENCED_CANCELLATION
              + "', 'oldStatus=CANCELLED, newStatus=CANCELLED')");
    }
  }

  private String legacyReservationInsert(UUID reservationId) {
    return "INSERT INTO reservations "
        + "(id, user_id, unit_id, start_date, end_date, status, created_at, updated_at, "
        + "price_per_night_snapshot, total_price, payment_currency, payment_exchange_rate, "
        + "guest_email, guest_message) "
        + "SELECT '"
        + reservationId
        + "', user_id, unit_id, start_date, end_date, 'CANCELLED', "
        + "TIMESTAMP '2026-10-01 10:00:00', TIMESTAMP '2026-10-22 10:00:00', "
        + "price_per_night_snapshot, total_price, payment_currency, payment_exchange_rate, "
        + "guest_email, guest_message FROM reservations LIMIT 1";
  }

  private LocalDateTime cancelledAt(UUID reservationId) throws SQLException {
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement =
            connection.prepareStatement("SELECT cancelled_at FROM reservations WHERE id = ?")) {
      statement.setObject(1, reservationId);
      try (var result = statement.executeQuery()) {
        result.next();
        Timestamp value = result.getTimestamp(1);
        return value == null ? null : value.toLocalDateTime();
      }
    }
  }

  private void insertConversation(UUID conversationId, UUID reservationId) throws SQLException {
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement =
            connection.prepareStatement(
                "INSERT INTO conversations "
                    + "(id, reservation_id, guest_user_id, owner_user_id) "
                    + "SELECT ?, ?, user_id, owner_id FROM reservations r "
                    + "JOIN units u ON u.id = r.unit_id "
                    + "JOIN properties p ON p.id = u.property_id WHERE r.id = ?")) {
      statement.setObject(1, conversationId);
      statement.setObject(2, reservationId);
      statement.setObject(3, reservationId);
      statement.executeUpdate();
    }
  }

  private void deleteReservation(UUID reservationId) throws SQLException {
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.prepareStatement("DELETE FROM reservations WHERE id = ?")) {
      statement.setObject(1, reservationId);
      statement.executeUpdate();
    }
  }
}
