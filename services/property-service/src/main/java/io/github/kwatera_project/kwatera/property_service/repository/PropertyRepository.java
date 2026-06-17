package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.dto.OwnerPropertyCountDto;
import io.github.kwatera_project.kwatera.property_service.model.Property;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
  List<Property> findByOwnerId(UUID ownerId);

  @Query(
      "SELECT new io.github.kwatera_project.kwatera.property_service.dto.OwnerPropertyCountDto(p.ownerId, COUNT(p)) "
          + "FROM Property p GROUP BY p.ownerId")
  List<OwnerPropertyCountDto> countPropertiesGroupByOwnerId();

  @Query(
      "SELECT new io.github.kwatera_project.kwatera.property_service.dto.OwnerPropertyCountDto(p.ownerId, COUNT(p)) "
          + "FROM Property p WHERE p.ownerId IN :ownerIds GROUP BY p.ownerId")
  List<OwnerPropertyCountDto> countPropertiesByOwnerIds(@Param("ownerIds") List<UUID> ownerIds);

  List<Property> findByLatitudeBetweenAndLongitudeBetween(
      BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng);

  @Query(
      value =
          "SELECT DISTINCT p.* FROM properties p WHERE "
              + "(:minLat IS NULL OR p.latitude BETWEEN :minLat AND :maxLat) AND "
              + "(:minLng IS NULL OR p.longitude BETWEEN :minLng AND :maxLng) AND "
              + "(COALESCE(:amenitiesLength, 0) = 0 OR "
              + "p.amenities @> CAST(COALESCE(:amenitiesJson, '[]') AS jsonb) OR "
              + "EXISTS ("
              + "  SELECT 1 FROM units u "
              + "  WHERE u.property_id = p.id "
              + "  AND u.amenities @> CAST(COALESCE(:amenitiesJson, '[]') AS jsonb)"
              + "))",
      nativeQuery = true)
  List<Property> findByBoundingBoxAndAmenities(
      @Param("minLat") BigDecimal minLat,
      @Param("maxLat") BigDecimal maxLat,
      @Param("minLng") BigDecimal minLng,
      @Param("maxLng") BigDecimal maxLng,
      @Param("amenitiesLength") Integer amenitiesLength,
      @Param("amenitiesJson") String amenitiesJson);
}
