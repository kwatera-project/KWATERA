package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.dto.CreateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewResponseDto;
import io.github.kwatera_project.kwatera.review_service.dto.UpdateReviewRequest;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
    private String expectedInternalToken;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {

        UUID authorId = validateAndGetUserId(authentication);

        boolean isGuest =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"));
        boolean isOwner =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"));

        if (!isGuest && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only guests and owners can write reviews");
        }

        return reviewService.createReview(authorId, request);
    }

    @GetMapping("/my")
    public List<ReviewResponseDto> getMyReviews(Authentication authentication) {
        UUID userId = validateAndGetUserId(authentication);

        boolean isGuest =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"));
        boolean isOwner =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"));

        if (!isGuest && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return reviewService.getReviewsByAuthor(userId);
    }

    @GetMapping("/{reviewId}")
    public ReviewResponseDto getReviewDetails(@PathVariable("reviewId") UUID reviewId, Authentication authentication) {
        return reviewService.getReviewDetails(reviewId);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponseDto updateReview(
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {


        UUID userId = validateAndGetUserId(authentication);
        boolean isGuest =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"));
        boolean isOwner =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"));

        if (!isGuest && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only guests and owners can update reviews");
        }

        return reviewService.updateReview(reviewId, userId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable("reviewId") UUID reviewId,
            Authentication authentication) {

        boolean isAdmin =
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        reviewService.deleteReview(reviewId, isAdmin);
    }

    @GetMapping("/internal/{reviewId}")
    public ReviewResponseDto getReviewDetailsInternal(
            @PathVariable("reviewId") UUID reviewId,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {

        if (internalToken == null || !internalToken.equals(expectedInternalToken)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: Invalid internal token");
        }
        return reviewService.getReviewDetailsInternal(reviewId);
    }

    private UUID validateAndGetUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof String) || ((String) details).trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
        }
        try {
            return UUID.fromString((String) details);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token format");
        }
    }
}
