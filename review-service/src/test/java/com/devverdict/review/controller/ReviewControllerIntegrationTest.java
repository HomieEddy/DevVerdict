package com.devverdict.review.controller;

import com.devverdict.review.dto.ReviewCreatedEvent;
import com.devverdict.review.dto.ReviewRequest;
import com.devverdict.review.dto.ReviewResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = { "review-updates" })
class ReviewControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine")
            .withDatabaseName("reviews_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private Consumer<String, ReviewCreatedEvent> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.devverdict.review.dto");

        DefaultKafkaConsumerFactory<String, ReviewCreatedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("review-updates"));
    }

    @Test
    void shouldCreateReviewAndReturn201() throws Exception {
        ReviewRequest request = new ReviewRequest(1L, "Great framework!", 5);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frameworkId").value(1))
                .andExpect(jsonPath("$.comment").value("Great framework!"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldPublishReviewCreatedEventToKafka() throws Exception {
        ReviewRequest request = new ReviewRequest(2L, "Solid choice for backend", 4);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ConsumerRecords<String, ReviewCreatedEvent> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ReviewCreatedEvent event = records.iterator().next().value();
        assertThat(event.frameworkId()).isEqualTo(2L);
        assertThat(event.rating()).isEqualTo(4);
    }

    @Test
    void shouldReturnReviewsForFrameworkOrderedByNewestFirst() throws Exception {
        // Create two reviews for the same framework
        ReviewRequest request1 = new ReviewRequest(3L, "First review", 3);
        ReviewRequest request2 = new ReviewRequest(3L, "Second review", 4);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        Thread.sleep(10); // Ensure different timestamps

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/framework/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].comment").value("Second review"))
                .andExpect(jsonPath("$[1].comment").value("First review"));
    }

    @Test
    void shouldReturnEmptyListWhenNoReviewsForFramework() throws Exception {
        mockMvc.perform(get("/api/reviews/framework/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectInvalidReviewRequest() throws Exception {
        ReviewRequest invalidRequest = new ReviewRequest(null, "", 6);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
