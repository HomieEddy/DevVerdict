package com.devverdict.review.service;

import com.devverdict.review.domain.OutboxEvent;
import com.devverdict.review.domain.Review;
import com.devverdict.review.domain.ReviewVote;
import com.devverdict.review.domain.VoteType;
import com.devverdict.review.dto.ReviewCreatedEvent;
import com.devverdict.review.dto.ReviewRequest;
import com.devverdict.review.dto.ReviewResponse;
import com.devverdict.review.repository.OutboxEventRepository;
import com.devverdict.review.repository.ReviewRepository;
import com.devverdict.review.repository.ReviewVoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private static final String TOPIC = "review-updates";

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ReviewService(ReviewRepository reviewRepository,
                         ReviewVoteRepository reviewVoteRepository,
                         OutboxEventRepository outboxRepository,
                         ObjectMapper objectMapper) {
        this.reviewRepository = reviewRepository;
        this.reviewVoteRepository = reviewVoteRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long authenticatedUserId) {
        Review review = new Review(
            request.frameworkId(),
            request.comment(),
            request.rating(),
            authenticatedUserId,
            request.username(),
            request.pros(),
            request.cons()
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
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = new OutboxEvent(
                "ReviewCreated",
                saved.getFrameworkId().toString(),
                payload
            );
            outboxRepository.save(outbox);
            logger.info("Enqueued ReviewCreated event id={} for review id={} in outbox", event.eventId(), saved.getId());
        } catch (Exception ex) {
            logger.error("Failed to enqueue ReviewCreated event for review id={}", saved.getId(), ex);
            throw new ReviewPublishException("Review saved but outbox enqueue failed", ex);
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
        review.setPros(request.pros());
        review.setCons(request.cons());
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
        return reviewRepository.findByFrameworkIdAndHiddenFalseOrderByCreatedAtDesc(frameworkId)
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByFramework(Long frameworkId, Pageable pageable) {
        return reviewRepository.findByFrameworkIdAndHiddenFalseOrderByCreatedAtDesc(frameworkId, pageable)
            .map(ReviewResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId)
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviewsForModeration() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(ReviewResponse::fromEntity)
            .toList();
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId, boolean hidden) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setHidden(hidden);
        Review updated = reviewRepository.save(review);
        logger.info("Moderated review id={}, hidden={}", reviewId, hidden);
        return ReviewResponse.fromEntity(updated);
    }

    @Transactional
    public ReviewResponse voteReview(Long reviewId, Long userId, VoteType voteType) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        Optional<ReviewVote> existingVote = reviewVoteRepository.findByReviewIdAndUserId(reviewId, userId);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Same vote = toggle off (remove vote)
                reviewVoteRepository.deleteByReviewIdAndUserId(reviewId, userId);
                if (voteType == VoteType.UPVOTE) {
                    review.decrementHelpfulVotes();
                } else {
                    review.decrementNotHelpfulVotes();
                }
            } else {
                // Different vote = switch
                vote.setVoteType(voteType);
                reviewVoteRepository.save(vote);
                if (voteType == VoteType.UPVOTE) {
                    review.incrementHelpfulVotes();
                    review.decrementNotHelpfulVotes();
                } else {
                    review.incrementNotHelpfulVotes();
                    review.decrementHelpfulVotes();
                }
            }
        } else {
            // New vote
            ReviewVote newVote = new ReviewVote(reviewId, userId, voteType);
            reviewVoteRepository.save(newVote);
            if (voteType == VoteType.UPVOTE) {
                review.incrementHelpfulVotes();
            } else {
                review.incrementNotHelpfulVotes();
            }
        }

        Review updated = reviewRepository.save(review);
        return ReviewResponse.fromEntity(updated);
    }
}
