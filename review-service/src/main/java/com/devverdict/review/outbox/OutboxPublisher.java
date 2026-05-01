package com.devverdict.review.outbox;

import com.devverdict.review.domain.OutboxEvent;
import com.devverdict.review.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "review-updates";
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository
                .findTop50ByPublishedAtIsNullAndRetryCountLessThanOrderByCreatedAtAsc(MAX_RETRIES);

        if (pending.isEmpty()) {
            return;
        }

        logger.info("Publishing {} pending outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                JsonNode payload = objectMapper.readTree(event.getPayload());
                String key = event.getAggregateId();

                kafkaTemplate.send(TOPIC, key, event.getPayload()).whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish outbox event id={}", event.getId(), ex);
                    } else {
                        logger.debug("Published outbox event id={} to partition={}",
                                event.getId(), result.getRecordMetadata().partition());
                    }
                });

                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
                logger.info("Marked outbox event id={} as published", event.getId());
            } catch (Exception ex) {
                event.incrementRetry();
                outboxRepository.save(event);
                logger.error("Error publishing outbox event id={} (retry={})",
                        event.getId(), event.getRetryCount(), ex);
            }
        }
    }
}
