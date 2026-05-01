package com.devverdict.review.dto;

import com.devverdict.review.domain.VoteType;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull VoteType voteType) {
}
