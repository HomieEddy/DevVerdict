package com.devverdict.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewCreatedEvent(
    UUID eventId,
    Long reviewId,
    Long frameworkId,
    Integer rating,
    Instant createdAt
) {
}
