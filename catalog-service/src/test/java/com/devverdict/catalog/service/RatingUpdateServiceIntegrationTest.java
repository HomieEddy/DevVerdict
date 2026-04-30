package com.devverdict.catalog.service;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.dto.ReviewCreatedEvent;
import com.devverdict.catalog.repository.FrameworkRepository;
import com.devverdict.catalog.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = { "review-updates" })
class RatingUpdateServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine")
            .withDatabaseName("catalog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaTemplate<String, ReviewCreatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        frameworkRepository.deleteAll();
        processedEventRepository.deleteAll();

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        DefaultKafkaProducerFactory<String, ReviewCreatedEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    void shouldUpdateAverageRatingWhenReviewCreatedEventReceived() {
        Framework framework = frameworkRepository.save(new Framework("Spring Boot", "Framework", "Java framework", 4.0));
        framework.setReviewCount(1);
        frameworkRepository.save(framework);

        ReviewCreatedEvent event = new ReviewCreatedEvent(
            UUID.randomUUID(), 1L, framework.getId(), 5, Instant.now()
        );

        kafkaTemplate.send("review-updates", framework.getId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Framework updated = frameworkRepository.findById(framework.getId()).orElseThrow();
            assertThat(updated.getAverageRating()).isEqualTo(4.5);
            assertThat(updated.getReviewCount()).isEqualTo(2);
        });
    }

    @Test
    void shouldHandleDuplicateEventsIdempotently() {
        Framework framework = frameworkRepository.save(new Framework("React", "Framework", "JS library", 3.0));
        framework.setReviewCount(2);
        frameworkRepository.save(framework);

        UUID eventId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(
            eventId, 2L, framework.getId(), 5, Instant.now()
        );

        kafkaTemplate.send("review-updates", framework.getId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Framework updated = frameworkRepository.findById(framework.getId()).orElseThrow();
            assertThat(updated.getAverageRating()).isEqualTo(3.6666666666666665);
            assertThat(updated.getReviewCount()).isEqualTo(3);
        });

        // Send duplicate event
        kafkaTemplate.send("review-updates", framework.getId().toString(), event);

        // Wait briefly and assert no change
        await().pollDelay(Duration.ofMillis(500)).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Framework updated = frameworkRepository.findById(framework.getId()).orElseThrow();
            assertThat(updated.getAverageRating()).isEqualTo(3.6666666666666665);
            assertThat(updated.getReviewCount()).isEqualTo(3);
            assertThat(processedEventRepository.count()).isEqualTo(1);
        });
    }

    @Test
    void shouldIgnoreEventForNonExistentFramework() {
        ReviewCreatedEvent event = new ReviewCreatedEvent(
            UUID.randomUUID(), 99L, 999L, 5, Instant.now()
        );

        kafkaTemplate.send("review-updates", "999", event);

        await().pollDelay(Duration.ofMillis(500)).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.count()).isZero();
        });
    }
}
