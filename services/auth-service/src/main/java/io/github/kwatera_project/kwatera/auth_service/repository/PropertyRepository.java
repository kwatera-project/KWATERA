package io.github.kwatera_project.kwatera.auth_service.repository;

import io.github.kwatera_project.kwatera.auth_service.model.Property;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

  @Query(
      value =
          "SELECT p.id, p.title, p.city, p.country, MIN(u.price_per_night) as price, p.image_url, p.description "
              + "FROM properties p "
              + "JOIN units u ON p.id = u.property_id "
              + "WHERE LOWER(p.city) IN (:cities) "
              + "GROUP BY p.id, p.title, p.city, p.country, p.image_url, p.description "
              + "LIMIT 3",
      nativeQuery = true)
  List<Object[]> findTop3PropertiesByCities(@Param("cities") List<String> cities);

  @Query(
      value =
          "SELECT p.id, p.title, p.city, p.country, MIN(u.price_per_night) as price, p.image_url, p.description "
              + "FROM properties p "
              + "JOIN units u ON p.id = u.property_id "
              + "GROUP BY p.id, p.title, p.city, p.country, p.image_url, p.description "
              + "LIMIT 3",
      nativeQuery = true)
  List<Object[]> findTop3DefaultProperties();
}
