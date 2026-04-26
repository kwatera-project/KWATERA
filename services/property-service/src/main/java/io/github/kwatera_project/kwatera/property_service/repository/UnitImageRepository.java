package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.UnitImage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitImageRepository extends JpaRepository<UnitImage, UUID> {

  Optional<UnitImage> findByUnitIdAndIsMainTrue(UUID unitId);
}
