package io.github.kwatera_project.kwatera.auth_service.repository;

import io.github.kwatera_project.kwatera.auth_service.model.NewsletterSubscriber;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

  Optional<NewsletterSubscriber> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<NewsletterSubscriber> findByToken(String token);

  List<NewsletterSubscriber> findByStatus(String status);
}
