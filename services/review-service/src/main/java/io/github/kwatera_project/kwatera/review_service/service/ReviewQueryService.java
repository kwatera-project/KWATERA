package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.dto.PublicReviewDto;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewPageDto;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewQueryService {

  private final ReviewRepository reviewRepository;
  private final PropertyClient propertyClient;

  public ReviewQueryService(ReviewRepository reviewRepository, PropertyClient propertyClient) {
    this.reviewRepository = reviewRepository;
    this.propertyClient = propertyClient;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ReviewPageDto getForUnit(UUID unitId, int page, int size) {
    validatePage(page, size);
    propertyClient.requireUnit(unitId);

    return getForUnits(List.of(unitId), page, size);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ReviewPageDto getForProperty(UUID propertyId, int page, int size) {
    validatePage(page, size);

    List<UUID> unitIds = propertyClient.getUnitIdsForProperty(propertyId);

    return getForUnits(unitIds, page, size);
  }

  private ReviewPageDto getForUnits(List<UUID> unitIds, int page, int size) {

    if (unitIds.isEmpty()) {
      return new ReviewPageDto(List.of(), page, size, 0, 0, null);
    }

    PageRequest pageable =
        PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    Page<Review> result = reviewRepository.findByUnitIdIn(unitIds, pageable);

    Double average = reviewRepository.averageRatingForUnits(unitIds);

    BigDecimal roundedAverage =
        average == null ? null : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);

    return new ReviewPageDto(
        result.getContent().stream().map(PublicReviewDto::from).toList(),
        page,
        size,
        result.getTotalPages(),
        result.getTotalElements(),
        roundedAverage);
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Page must be at least 0 and size must be between 1 and 100");
    }
  }
}
