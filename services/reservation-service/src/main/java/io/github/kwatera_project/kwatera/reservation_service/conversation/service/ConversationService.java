package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import io.github.kwatera_project.kwatera.reservation_service.conversation.client.PropertyOwnershipClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.ConversationResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.ConversationRepository;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConversationService {

  private static final String NOT_FOUND = "Conversation or reservation not found";

  private final ConversationRepository conversationRepository;
  private final ReservationRepository reservationRepository;
  private final PropertyOwnershipClient propertyOwnershipClient;
  private final ConversationLifecyclePolicy lifecyclePolicy;
  private final ConversationParticipantAuthorizer participantAuthorizer;

  @Transactional
  public ConversationResponse getOrCreate(UUID reservationId, UUID currentUserId) {
    Reservation reservation =
        reservationRepository.findByIdForUpdate(reservationId).orElseThrow(this::notFound);

    Conversation conversation =
        conversationRepository.findByReservationId(reservationId).orElse(null);
    if (conversation != null) {
      participantAuthorizer.requireConversationParticipant(conversation, currentUserId);
      lifecyclePolicy.requireAvailable(reservation, conversation);
      return toResponse(conversation, reservation);
    }

    UUID ownerId = propertyOwnershipClient.getOwnerIdForUnit(reservation.getUnitId());
    participantAuthorizer.requireReservationParticipant(reservation, ownerId, currentUserId);

    lifecyclePolicy.requireAvailable(reservation, null);

    Conversation created = new Conversation();
    created.setReservationId(reservation.getId());
    created.setGuestUserId(reservation.getUserId());
    created.setOwnerUserId(ownerId);
    created = conversationRepository.saveAndFlush(created);
    return toResponse(created, reservation);
  }

  @Transactional(readOnly = true)
  public ConversationResponse get(UUID conversationId, UUID currentUserId) {
    Conversation conversation =
        conversationRepository
            .findAccessibleById(conversationId, currentUserId)
            .orElseThrow(this::notFound);
    participantAuthorizer.requireConversationParticipant(conversation, currentUserId);
    Reservation reservation =
        reservationRepository.findById(conversation.getReservationId()).orElseThrow(this::notFound);
    lifecyclePolicy.requireAvailable(reservation, conversation);
    return toResponse(conversation, reservation);
  }

  @Transactional(readOnly = true)
  public List<ConversationResponse> list(UUID currentUserId) {
    List<Conversation> conversations =
        conversationRepository.findAllAccessibleByUserId(currentUserId);
    Map<UUID, Reservation> reservations =
        reservationRepository
            .findAllById(conversations.stream().map(Conversation::getReservationId).toList())
            .stream()
            .collect(Collectors.toMap(Reservation::getId, Function.identity()));

    return conversations.stream()
        .filter(
            conversation -> {
              Reservation reservation = reservations.get(conversation.getReservationId());
              return reservation != null && lifecyclePolicy.isAvailable(reservation, conversation);
            })
        .map(
            conversation ->
                toResponse(conversation, reservations.get(conversation.getReservationId())))
        .toList();
  }

  private ConversationResponse toResponse(Conversation conversation, Reservation reservation) {
    return new ConversationResponse(
        conversation.getId(),
        conversation.getReservationId(),
        conversation.getGuestUserId(),
        conversation.getOwnerUserId(),
        reservation.getStatus(),
        conversation.getCreatedAt(),
        conversation.getLastMessageAt(),
        lifecyclePolicy.retentionExpiresOn(reservation, conversation).orElse(null));
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
  }
}
