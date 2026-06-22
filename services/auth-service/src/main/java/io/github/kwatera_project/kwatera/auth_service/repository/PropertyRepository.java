package io.github.kwatera_project.kwatera.auth_service.repository;

import io.github.kwatera_project.kwatera.auth_service.model.Property;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Read-only newsletter projection repository.
 *
 * <p>Auth-service has SELECT-only access to the {@code properties} table, which is owned by
 * property-service. This repository is used exclusively to fetch preference-based property
 * recommendations for the weekly personalised newsletter. Do not add write operations here.
 */
@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

  /** Returns up to 3 properties whose city matches the subscriber's travel-preference cities. */
  @Query(
      value =
          "SELECT p.id, p.title, p.city, p.country, MIN(u.price_per_night) as price, pi.url as image_url, p.description "
              + "FROM properties p "
              + "JOIN units u ON p.id = u.property_id "
              + "LEFT JOIN property_images pi ON p.id = pi.property_id AND pi.is_main = true "
              + "WHERE LOWER(p.city) IN (:cities) "
              + "GROUP BY p.id, p.title, p.city, p.country, pi.url, p.description "
              + "LIMIT 3",
      nativeQuery = true)
  List<Object[]> findRecommendedPropertiesByCities(@Param("cities") List<String> cities);

  /**
   * Returns up to 3 properties as a general recommendation fallback when no preference is known.
   */
  @Query(
      value =
          "SELECT p.id, p.title, p.city, p.country, MIN(u.price_per_night) as price, pi.url as image_url, p.description "
              + "FROM properties p "
              + "JOIN units u ON p.id = u.property_id "
              + "LEFT JOIN property_images pi ON p.id = pi.property_id AND pi.is_main = true "
              + "GROUP BY p.id, p.title, p.city, p.country, pi.url, p.description "
              + "LIMIT 3",
      nativeQuery = true)
  List<Object[]> findDefaultRecommendedProperties();
}
