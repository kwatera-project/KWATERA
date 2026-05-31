package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.Unit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

  List<Unit> findByPropertyId(UUID propertyId);

  List<Unit> findByPropertyIdIn(List<UUID> propertyIds);

  Optional<Unit> findByIdAndPropertyId(UUID id, UUID propertyId);

  @Modifying
  @Query("delete from Unit u where u.propertyId = :propertyId")
  void deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
