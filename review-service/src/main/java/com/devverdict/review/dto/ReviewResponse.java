package com.devverdict.review.dto;

import com.devverdict.review.domain.Review;

import java.time.Instant;

public record ReviewResponse(
    Long id,
    Long frameworkId,
    String comment,
    Integer rating,
    Instant createdAt
) {

    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getFrameworkId(),
            review.getComment(),
            review.getRating(),
            review.getCreatedAt()
        );
    }
}
