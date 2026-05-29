package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReadingUploadAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaReadingUploadAttemptRepository
    extends JpaRepository<MediaReadingUploadAttempt, UUID> {

  List<MediaReadingUploadAttempt> findByMediaReadingIdOrderByAttemptedAtAsc(UUID mediaReadingId);
}
