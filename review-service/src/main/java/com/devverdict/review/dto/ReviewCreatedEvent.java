package com.devverdict.review.dto;

import java.time.Instant;

public record ReviewCreatedEvent(
    Long reviewId,
    Long frameworkId,
    Integer rating,
    Instant createdAt
) {
}
