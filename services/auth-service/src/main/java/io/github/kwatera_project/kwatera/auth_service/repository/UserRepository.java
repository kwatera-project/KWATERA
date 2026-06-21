package io.github.kwatera_project.kwatera.auth_service.repository;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  long countByRole(Role role);

  @Query(
      "SELECT u FROM User u WHERE "
          + "(:role IS NULL OR u.role = :role) AND "
          + "(:search IS NULL OR :search = '' OR "
          + " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<User> findAllFilteredAndSearched(
      @Param("role") Role role, @Param("search") String search, Pageable pageable);

  @Query(
      value =
          "SELECT p.city, CAST(p.amenities AS VARCHAR) FROM reservations r "
              + "JOIN units u ON r.unit_id = u.id "
              + "JOIN properties p ON u.property_id = p.id "
              + "WHERE r.user_id = :userId",
      nativeQuery = true)
  List<Object[]> findPropertyDetailsByUserId(@Param("userId") UUID userId);
}
