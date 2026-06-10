package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.UnitImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitImageRepository extends JpaRepository<UnitImage, UUID> {

  Optional<UnitImage> findByUnitIdAndIsMainTrue(UUID unitId);

  List<UnitImage> findByUnitId(UUID unitId);

  @Modifying
  @Query(
      """
       update UnitImage u
       set u.isMain = false
       where u.unitId = :unitId
       """)
  void clearMainImage(@Param("unitId") UUID unitId);
}
