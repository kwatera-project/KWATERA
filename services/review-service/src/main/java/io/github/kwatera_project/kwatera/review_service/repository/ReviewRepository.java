package io.github.kwatera_project.kwatera.review_service.repository;

import io.github.kwatera_project.kwatera.review_service.model.Review;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findReviewsByAuthorId(UUID authorId);

  boolean existsByReservationId(UUID reservationId);

  Page<Review> findByUnitIdIn(Collection<UUID> unitIds, Pageable pageable);

  @Query(
      """
      SELECT AVG(review.rating)
      FROM Review review
      WHERE review.unitId IN :unitIds
      """)
  Double averageRatingForUnits(@Param("unitIds") Collection<UUID> unitIds);
}
