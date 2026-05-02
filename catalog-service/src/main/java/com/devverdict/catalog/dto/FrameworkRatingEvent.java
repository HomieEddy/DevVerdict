package com.devverdict.catalog.dto;

public record FrameworkRatingEvent(
    String type,
    Long frameworkId,
    Double newAverage,
    Integer newCount
) {}
