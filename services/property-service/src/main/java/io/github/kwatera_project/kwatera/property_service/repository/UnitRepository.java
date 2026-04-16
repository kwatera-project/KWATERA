package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.Unit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

  List<Unit> findByPropertyId(UUID propertyId);
}
