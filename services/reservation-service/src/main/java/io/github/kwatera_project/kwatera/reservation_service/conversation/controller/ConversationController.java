package io.github.kwatera_project.kwatera.reservation_service.conversation.controller;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.ConversationResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationAuthenticationService;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Reservation-based guest and owner conversations")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

  private final ConversationService conversationService;
  private final ConversationAuthenticationService authenticationService;

  @PutMapping("/api/v1/reservations/{reservationId}/conversation")
  @Operation(summary = "Get or create the conversation for a reservation")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conversation returned"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "404", description = "Conversation or reservation not found"),
    @ApiResponse(responseCode = "409", description = "Conversation unavailable for lifecycle"),
    @ApiResponse(responseCode = "502", description = "Property data is inconsistent"),
    @ApiResponse(responseCode = "503", description = "Property service is unavailable")
  })
  public ConversationResponse getOrCreate(
      @PathVariable("reservationId") UUID reservationId, Authentication authentication) {
    return conversationService.getOrCreate(
        reservationId, authenticationService.requireUserId(authentication));
  }

  @GetMapping("/api/v1/conversations")
  @Operation(summary = "List conversations accessible to the current user")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conversations returned"),
    @ApiResponse(responseCode = "401", description = "Authentication required")
  })
  public List<ConversationResponse> list(Authentication authentication) {
    return conversationService.list(authenticationService.requireUserId(authentication));
  }

  @GetMapping("/api/v1/conversations/{conversationId}")
  @Operation(summary = "Get one conversation accessible to the current user")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conversation returned"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "404", description = "Conversation not found"),
    @ApiResponse(responseCode = "409", description = "Conversation unavailable for lifecycle")
  })
  public ConversationResponse get(
      @PathVariable("conversationId") UUID conversationId, Authentication authentication) {
    return conversationService.get(
        conversationId, authenticationService.requireUserId(authentication));
  }
}
