package com.devverdict.review.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
    @NotNull(message = "Framework ID is required")
    Long frameworkId,

    @NotBlank(message = "Comment is required")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    String comment,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Integer rating,

    Long userId,

    String username,

    @Size(max = 500, message = "Pros must be at most 500 characters")
    String pros,

    @Size(max = 500, message = "Cons must be at most 500 characters")
    String cons
) {
}
