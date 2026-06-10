package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.PropertyImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

  Optional<PropertyImage> findByPropertyIdAndIsMainTrue(UUID propertyId);

  List<PropertyImage> findByPropertyId(UUID propertyId);

  @Modifying
  @Query(
      """
       update PropertyImage p
       set p.isMain = false
       where p.propertyId = :propertyId
       """)
  void clearMainImage(@Param("propertyId") UUID propertyId);
}
