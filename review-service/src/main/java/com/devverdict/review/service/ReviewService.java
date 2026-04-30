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
            request.rating()
        );

        Review saved = reviewRepository.save(review);
        logger.info("Saved review id={} for framework id={}", saved.getId(), saved.getFrameworkId());

        ReviewCreatedEvent event = new ReviewCreatedEvent(
            saved.getId(),
            saved.getFrameworkId(),
            saved.getRating(),
            saved.getCreatedAt()
        );

        try {
            kafkaTemplate.send(TOPIC, saved.getFrameworkId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish ReviewCreated event for review id={}", saved.getId(), ex);
                    } else {
                        logger.info("Published ReviewCreated event for review id={} to topic={}",
                            saved.getId(), result.getRecordMetadata().topic());
                    }
                });
        } catch (Exception ex) {
            logger.error("Exception while sending Kafka message for review id={}", saved.getId(), ex);
            throw new ReviewPublishException("Review saved but event publication failed", ex);
        }

        return ReviewResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByFramework(Long frameworkId) {
        return reviewRepository.findByFrameworkIdOrderByCreatedAtDesc(frameworkId)
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }
}
