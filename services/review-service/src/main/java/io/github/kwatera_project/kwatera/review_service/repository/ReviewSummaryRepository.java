package io.github.kwatera_project.kwatera.review_service.repository;

import io.github.kwatera_project.kwatera.review_service.model.ReviewSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, UUID> {

  @Query("select s.unitId from ReviewSummary s where s.unitId is not null")
  List<UUID> findCachedUnitIds();

  @Query("select s.propertyId from ReviewSummary s where s.propertyId is not null")
  List<UUID> findCachedPropertyIds();

  Optional<ReviewSummary> findByUnitId(UUID unitId);

  Optional<ReviewSummary> findByPropertyId(UUID propertyId);
}
