package com.devverdict.review.controller;

import com.devverdict.review.dto.ReviewRequest;
import com.devverdict.review.dto.ReviewResponse;
import com.devverdict.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.created(URI.create("/api/reviews/" + response.id())).body(response);
    }

    @GetMapping("/framework/{frameworkId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByFramework(@PathVariable Long frameworkId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByFramework(frameworkId);
        return ResponseEntity.ok(reviews);
    }
}
