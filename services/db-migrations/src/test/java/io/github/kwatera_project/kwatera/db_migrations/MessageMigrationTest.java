package io.github.kwatera_project.kwatera.db_migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class MessageMigrationTest {
  @Test
  void migrationEnforcesMessageConstraintsAndSupportsOrderedHistory() throws Exception {
    try (var connection =
        DriverManager.getConnection(
            "jdbc:h2:mem:messages_" + UUID.randomUUID() + ";MODE=PostgreSQL")) {
      try (var statement = connection.createStatement()) {
        statement.execute("CREATE TABLE conversations (id UUID PRIMARY KEY)");
      }
      UUID conversation = UUID.randomUUID();
      try (var statement = connection.prepareStatement("INSERT INTO conversations VALUES (?)")) {
        statement.setObject(1, conversation);
        statement.executeUpdate();
      }
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/migration/V38__create_conversation_messages.sql"));
      verifyConstraintsAndOrdering(connection, conversation);
    }
  }

  // The same assertions run against the complete Flyway chain on PostgreSQL when Docker is
  // available.
  static void verifyConstraintsAndOrdering(Connection connection, UUID conversation)
      throws Exception {
    UUID sender = UUID.randomUUID(); // Intentionally absent from any service-owned users table.
    Instant time = Instant.parse("2026-10-27T12:00:00.123456Z");
    UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID high = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    insert(connection, low, conversation, sender, "a".repeat(4000), time);
    insert(connection, high, conversation, sender, "hello", time);
    try (var statement =
        connection.prepareStatement(
            "SELECT id, created_at FROM conversation_messages WHERE conversation_id = ? ORDER BY created_at DESC, id DESC")) {
      statement.setObject(1, conversation);
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getObject(1, UUID.class)).isEqualTo(high);
        assertThat(result.getObject(2, OffsetDateTime.class).toInstant()).isEqualTo(time);
        assertThat(result.next()).isTrue();
        assertThat(result.getObject(1, UUID.class)).isEqualTo(low);
        assertThat(result.next()).isFalse();
      }
    }
    for (String content : new String[] {null, "", "a".repeat(4001)}) {
      assertThatThrownBy(
              () -> insert(connection, UUID.randomUUID(), conversation, sender, content, time))
          .isInstanceOf(SQLException.class);
    }
    assertThatThrownBy(() -> insert(connection, null, conversation, sender, "hello", time))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> insert(connection, UUID.randomUUID(), null, sender, "hello", time))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(
            () -> insert(connection, UUID.randomUUID(), conversation, null, "hello", time))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(
            () -> insert(connection, UUID.randomUUID(), conversation, sender, "hello", null))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(
            () -> insert(connection, UUID.randomUUID(), UUID.randomUUID(), sender, "hello", time))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> insert(connection, high, conversation, sender, "duplicate", time))
        .isInstanceOf(SQLException.class);
    String table =
        connection.getMetaData().storesUpperCaseIdentifiers()
            ? "CONVERSATION_MESSAGES"
            : "conversation_messages";
    var columns = new java.util.ArrayList<String>();
    var directions = new java.util.ArrayList<String>();
    try (var metadata = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
      while (metadata.next()) {
        if ("idx_messages_conversation_history"
            .equalsIgnoreCase(metadata.getString("INDEX_NAME"))) {
          columns.add(metadata.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT));
          directions.add(metadata.getString("ASC_OR_DESC"));
        }
      }
    }
    assertThat(columns).containsExactly("conversation_id", "created_at", "id");
    assertThat(directions).containsExactly("A", "D", "D");
  }

  private static void insert(
      Connection connection, UUID id, UUID conversation, UUID sender, String content, Instant time)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO conversation_messages (id, conversation_id, sender_user_id, content, created_at) VALUES (?, ?, ?, ?, ?)")) {
      statement.setObject(1, id);
      statement.setObject(2, conversation);
      statement.setObject(3, sender);
      statement.setString(4, content);
      statement.setObject(5, time == null ? null : time.atOffset(ZoneOffset.UTC));
      statement.executeUpdate();
    }
  }
}
