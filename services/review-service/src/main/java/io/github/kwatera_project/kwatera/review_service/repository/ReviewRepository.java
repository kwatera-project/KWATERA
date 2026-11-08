package io.github.kwatera_project.kwatera.review_service.repository;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
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

  @Query("SELECT DISTINCT review.unitId FROM Review review ORDER BY review.unitId")
  List<UUID> findUnitIdsWithReviews();

  @Query(
      """
            SELECT new io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput(
                review.id, review.comment, COALESCE(review.updatedAt, review.createdAt))
            FROM Review review
            WHERE review.unitId IN :unitIds
              AND review.comment IS NOT NULL
              AND TRIM(review.comment) <> ''
            ORDER BY review.id
            """)
  List<ReviewSummaryInput> findSummaryInputsByUnitIds(@Param("unitIds") Collection<UUID> unitIds);

  @Query(
      """
      SELECT AVG(review.rating)
      FROM Review review
      WHERE review.unitId IN :unitIds
      """)
  Double averageRatingForUnits(@Param("unitIds") Collection<UUID> unitIds);
}
