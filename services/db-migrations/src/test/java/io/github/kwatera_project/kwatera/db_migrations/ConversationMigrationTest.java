package io.github.kwatera_project.kwatera.db_migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ConversationMigrationTest {

  private static final UUID RESERVATION_WITH_HISTORY =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID LEGACY_RESERVATION =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID UNKNOWN_CANCELLATION_RESERVATION =
      UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID GUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  private Connection connection;

  @BeforeEach
  void setUp() throws SQLException {
    connection =
        DriverManager.getConnection(
            "jdbc:h2:mem:v34_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    try (Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
      statement.execute(
          "CREATE TABLE reservations (id UUID PRIMARY KEY, status VARCHAR(50) NOT NULL)");
      statement.execute(
          "CREATE TABLE reservation_status_history (reservation_id UUID NOT NULL, "
              + "new_status VARCHAR(50) NOT NULL, changed_at TIMESTAMP NOT NULL)");
      statement.execute(
          "CREATE TABLE system_events (id UUID PRIMARY KEY, timestamp TIMESTAMP NOT NULL, "
              + "action_type VARCHAR(80) NOT NULL, entity_type VARCHAR(80), "
              + "entity_id UUID, details TEXT)");
      statement.execute("INSERT INTO users VALUES ('" + GUEST_ID + "'), ('" + OWNER_ID + "')");
      statement.execute(
          "INSERT INTO reservations VALUES ('"
              + RESERVATION_WITH_HISTORY
              + "', 'CANCELLED'), ('"
              + LEGACY_RESERVATION
              + "', 'CANCELLED'), ('"
              + UNKNOWN_CANCELLATION_RESERVATION
              + "', 'CANCELLED')");
      statement.execute(
          "INSERT INTO reservation_status_history VALUES ('"
              + RESERVATION_WITH_HISTORY
              + "', 'CANCELLED', TIMESTAMP '2026-10-20 12:30:00')");
      statement.execute(
          "INSERT INTO system_events VALUES (RANDOM_UUID(), "
              + "TIMESTAMP '2026-10-21 14:45:00', 'EXPIRED_RESERVATION_CANCELLED', "
              + "'RESERVATION', '"
              + LEGACY_RESERVATION
              + "', 'oldStatus=PENDING, newStatus=CANCELLED')");
    }
  }

  @AfterEach
  void tearDown() throws SQLException {
    connection.close();
  }

  @Test
  void shouldApplyConversationSchemaBackfillAndUniqueness() throws SQLException {
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("db/migration/V34__create_conversations.sql"));
    ScriptUtils.executeSqlScript(
        connection,
        new ClassPathResource("db/migration/V35__backfill_cancelled_at_from_system_events.sql"));

    assertThat(cancelledAt(RESERVATION_WITH_HISTORY))
        .isEqualTo(LocalDateTime.of(2026, 10, 20, 12, 30));
    assertThat(cancelledAt(LEGACY_RESERVATION)).isEqualTo(LocalDateTime.of(2026, 10, 21, 14, 45));
    assertThat(cancelledAt(UNKNOWN_CANCELLATION_RESERVATION)).isNull();

    insertConversation(UUID.randomUUID(), RESERVATION_WITH_HISTORY);
    assertThatThrownBy(() -> insertConversation(UUID.randomUUID(), RESERVATION_WITH_HISTORY))
        .isInstanceOf(SQLException.class);
  }

  private LocalDateTime cancelledAt(UUID reservationId) throws SQLException {
    try (var statement =
        connection.prepareStatement("SELECT cancelled_at FROM reservations WHERE id = ?")) {
      statement.setObject(1, reservationId);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getObject(1, LocalDateTime.class);
      }
    }
  }

  private void insertConversation(UUID conversationId, UUID reservationId) throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO conversations "
                + "(id, reservation_id, guest_user_id, owner_user_id) VALUES (?, ?, ?, ?)")) {
      statement.setObject(1, conversationId);
      statement.setObject(2, reservationId);
      statement.setObject(3, GUEST_ID);
      statement.setObject(4, OWNER_ID);
      statement.executeUpdate();
    }
  }
}
