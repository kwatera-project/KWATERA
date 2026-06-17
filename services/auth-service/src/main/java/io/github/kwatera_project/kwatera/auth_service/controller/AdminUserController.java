package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.auth_service.dto.AdminUserKpiDto;
import io.github.kwatera_project.kwatera.auth_service.dto.AdminUserResponseDto;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final UserRepository userRepository;
  private final PropertyClient propertyClient;

  @GetMapping("/kpis")
  public ResponseEntity<AdminUserKpiDto> getAdminUserKpis() {
    long totalUsers = userRepository.count();
    long totalGuests = userRepository.countByRole(Role.GUEST);
    long totalOwners = userRepository.countByRole(Role.OWNER);
    long totalProperties = propertyClient.getTotalPropertiesCount();

    return ResponseEntity.ok(
        new AdminUserKpiDto(totalUsers, totalGuests, totalOwners, totalProperties));
  }

  @GetMapping
  public ResponseEntity<Page<AdminUserResponseDto>> getAdminUsers(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size,
      @RequestParam(name = "role", required = false) Role role,
      @RequestParam(name = "search", required = false) String search) {

    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

    Page<User> userPage =
        userRepository.findAllFilteredAndSearched(role, normalizedSearch, pageable);

    List<UUID> ownerIds =
        userPage.getContent().stream()
            .filter(u -> u.getRole() == Role.OWNER)
            .map(User::getId)
            .toList();

    Map<UUID, Long> propertyCounts = new HashMap<>();
    if (!ownerIds.isEmpty()) {
      propertyCounts = propertyClient.getOwnerPropertyCounts(ownerIds);
    }

    final Map<UUID, Long> finalPropertyCounts = propertyCounts;

    Page<AdminUserResponseDto> responsePage =
        userPage.map(
            user -> {
              long propertyCount = 0;
              if (user.getRole() == Role.OWNER) {
                propertyCount = finalPropertyCounts.getOrDefault(user.getId(), 0L);
              }
              return new AdminUserResponseDto(
                  user.getId(),
                  user.getFirstName(),
                  user.getLastName(),
                  user.getEmail(),
                  user.getRole(),
                  user.isEnabled() ? "Active" : "Inactive",
                  user.getCreatedAt(),
                  propertyCount);
            });

    return ResponseEntity.ok(responsePage);
  }
}
