package io.github.kwatera_project.kwatera.review_service.repository;

import io.github.kwatera_project.kwatera.review_service.model.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findReviewsByAuthorId(UUID authorId);
}
