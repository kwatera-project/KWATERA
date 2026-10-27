package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"items", "nextCursor", "hasMore"})
public record MessageHistoryResponse(
    List<MessageResponse> items,
    @Schema(
            description = "Opaque boundary for older messages; null on the last page",
            nullable = true)
        String nextCursor,
    boolean hasMore) {
  public MessageHistoryResponse {
    items = List.copyOf(items);
  }
}
