package io.github.kwatera_project.kwatera.reservation_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.AdminReservationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

  private final AdminReservationService adminReservationService;

  @GetMapping
  public List<ReservationOverviewDto> getReservations(
      @RequestParam(name = "status", required = false) ReservationStatus status,
      HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    UUID ownerId = null;
    boolean isAdmin = false;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      String[] chunks = token.split("\\.");

      if (chunks.length > 1) {
        try {
          Base64.Decoder decoder = Base64.getUrlDecoder();
          String payload =
              new String(decoder.decode(chunks[1]), java.nio.charset.StandardCharsets.UTF_8);

          ObjectMapper mapper = new ObjectMapper();
          JsonNode payloadNode = mapper.readTree(payload);

          if (payloadNode.has("userId")) {
            String userIdString = payloadNode.get("userId").asText();
            ownerId = UUID.fromString(userIdString);
          } else {
            System.err.println("No userID in token. Login again");
          }

          if (payloadNode.has("role")) {
            JsonNode rolesNode = payloadNode.get("role");
            for (JsonNode roleNode : rolesNode) {
              if ("ROLE_ADMIN".equals(roleNode.asText())) {
                isAdmin = true;
                break;
              }
            }
          }

        } catch (Exception e) {
          System.err.println("Parssing error: " + e.getMessage());
        }
      }
    }

    if (ownerId == null) {
      throw new RuntimeException("Unauthorized: Token is incorrect");
    }

    return adminReservationService.getReservationsOverview(ownerId, status, isAdmin);
  }
}
