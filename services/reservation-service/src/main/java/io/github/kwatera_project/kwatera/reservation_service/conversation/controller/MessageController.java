package io.github.kwatera_project.kwatera.reservation_service.conversation.controller;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.*;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationAuthenticationService;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@Tag(name = "Conversations")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
  @ApiResponse(
      responseCode = "400",
      description = "Invalid content, identifier, request, limit or cursor"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(
      responseCode = "404",
      description = "Conversation or reservation not found (also for non-participants)"),
  @ApiResponse(
      responseCode = "409",
      description = "Conversation unavailable under reservation lifecycle or retention")
})
public class MessageController {
  private final MessageService messages;
  private final ConversationAuthenticationService authenticationService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Persist a message from the authenticated participant",
      description =
          "Content is normalized with String.strip(); requires 1–4000 normalized Java characters. Sender, ID and timestamp are server-generated. Does not create conversations.")
  @ApiResponse(
      responseCode = "201",
      description = "Persisted message",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MessageResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          """
              {"id":"11111111-1111-1111-1111-111111111111",
               "conversationId":"22222222-2222-2222-2222-222222222222",
               "senderUserId":"33333333-3333-3333-3333-333333333333",
               "content":"Hello, what time can we check in?","createdAt":"2026-10-27T12:00:00.123456Z"}
              """)))
  public MessageResponse send(
      @PathVariable("conversationId") UUID conversationId,
      @Valid @RequestBody SendMessageRequest request,
      Authentication authentication) {
    return messages.send(
        conversationId, authenticationService.requireUserId(authentication), request.content());
  }

  @GetMapping
  @Operation(
      summary = "Retrieve a bounded page of conversation history",
      description =
          "Newest first, ordered by (createdAt DESC, id DESC). Omit cursor for newest messages; pass nextCursor unchanged to retrieve strictly older messages. Cursors are scoped to the conversation. An empty/final page has nextCursor=null and hasMore=false. Access is checked before limit/cursor validation.")
  @ApiResponse(
      responseCode = "200",
      description = "Message history",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MessageHistoryResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          """
              {"items":[{"id":"11111111-1111-1111-1111-111111111111",
               "conversationId":"22222222-2222-2222-2222-222222222222",
               "senderUserId":"33333333-3333-3333-3333-333333333333",
               "content":"Hello, what time can we check in?","createdAt":"2026-10-27T12:00:00.123456Z"}],
               "nextCursor":null,"hasMore":false}
              """)))
  public MessageHistoryResponse history(
      @PathVariable("conversationId") UUID conversationId,
      @Parameter(
              description = "Page size (integer, default 50, range 1–100)",
              schema =
                  @Schema(type = "integer", defaultValue = "50", minimum = "1", maximum = "100"))
          @RequestParam(name = "limit", required = false)
          String limit,
      @Parameter(description = "Opaque nextCursor from the preceding page; omit on first request")
          @RequestParam(name = "cursor", required = false)
          String cursor,
      Authentication authentication) {
    return messages.history(
        conversationId, authenticationService.requireUserId(authentication), limit, cursor);
  }
}
