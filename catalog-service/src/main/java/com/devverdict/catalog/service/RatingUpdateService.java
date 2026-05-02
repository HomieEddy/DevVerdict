package com.devverdict.catalog.service;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.domain.ProcessedEvent;
import com.devverdict.catalog.dto.FrameworkRatingEvent;
import com.devverdict.catalog.dto.ReviewCreatedEvent;
import com.devverdict.catalog.repository.FrameworkRepository;
import com.devverdict.catalog.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(RatingUpdateService.class);
    private static final String TOPIC = "review-updates";

    private final FrameworkRepository frameworkRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final SseBroadcastService sseBroadcastService;

    public RatingUpdateService(FrameworkRepository frameworkRepository,
                               ProcessedEventRepository processedEventRepository,
                               SseBroadcastService sseBroadcastService) {
        this.frameworkRepository = frameworkRepository;
        this.processedEventRepository = processedEventRepository;
        this.sseBroadcastService = sseBroadcastService;
    }

    @KafkaListener(topics = TOPIC, groupId = "catalog-rating-consumer")
    @Transactional
    public void onReviewCreated(ReviewCreatedEvent event) {
        if (event == null || event.eventId() == null) {
            logger.warn("Received invalid ReviewCreatedEvent with null eventId");
            return;
        }

        String eventIdStr = event.eventId().toString();

        if (processedEventRepository.existsByEventId(eventIdStr)) {
            logger.info("Duplicate event id={} already processed, skipping", eventIdStr);
            return;
        }

        int updated = frameworkRepository.updateAverageRating(event.frameworkId(), event.rating());

        if (updated == 0) {
            logger.warn("Framework id={} not found for rating update, event id={}", event.frameworkId(), eventIdStr);
            return;
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
        logger.info("Updated average rating for framework id={} with rating={}, event id={}",
            event.frameworkId(), event.rating(), eventIdStr);

        frameworkRepository.findById(event.frameworkId()).ifPresent(framework -> {
            FrameworkRatingEvent ratingEvent = new FrameworkRatingEvent(
                "REVIEW_CREATED",
                framework.getId(),
                framework.getAverageRating(),
                framework.getReviewCount()
            );
            sseBroadcastService.broadcast(framework.getId(), ratingEvent);
            logger.info("Broadcasted SSE rating update for framework id={}", framework.getId());
        });
    }
}
