package com.devverdict.review.service;

import com.devverdict.review.domain.Review;
import com.devverdict.review.dto.ReviewCreatedEvent;
import com.devverdict.review.dto.ReviewRequest;
import com.devverdict.review.dto.ReviewResponse;
import com.devverdict.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private static final String TOPIC = "review-updates";

    private final ReviewRepository reviewRepository;
    private final KafkaTemplate<String, ReviewCreatedEvent> kafkaTemplate;

    public ReviewService(ReviewRepository reviewRepository,
                         KafkaTemplate<String, ReviewCreatedEvent> kafkaTemplate) {
        this.reviewRepository = reviewRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        Review review = new Review(
            request.frameworkId(),
            request.comment(),
            request.rating(),
            request.userId(),
            request.username()
        );

        Review saved = reviewRepository.save(review);
        logger.info("Saved review id={} for framework id={}", saved.getId(), saved.getFrameworkId());

        ReviewCreatedEvent event = new ReviewCreatedEvent(
            UUID.randomUUID(),
            saved.getId(),
            saved.getFrameworkId(),
            saved.getRating(),
            saved.getCreatedAt()
        );

        try {
            kafkaTemplate.send(TOPIC, saved.getFrameworkId().toString(), event).get();
            logger.info("Published ReviewCreated event id={} for review id={}", event.eventId(), saved.getId());
        } catch (Exception ex) {
            logger.error("Failed to publish ReviewCreated event for review id={}", saved.getId(), ex);
            throw new ReviewPublishException("Review saved but event publication failed", ex);
        }

        return ReviewResponse.fromEntity(saved);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (review.getUserId() == null || !review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to edit this review");
        }

        review.setComment(request.comment());
        review.setRating(request.rating());
        Review updated = reviewRepository.save(review);
        return ReviewResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (review.getUserId() == null || !review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this review");
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByFramework(Long frameworkId) {
        return reviewRepository.findByFrameworkIdOrderByCreatedAtDesc(frameworkId)
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }
}
