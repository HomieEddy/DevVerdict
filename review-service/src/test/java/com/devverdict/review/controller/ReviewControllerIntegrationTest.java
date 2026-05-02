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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        ReviewRequest request = new ReviewRequest(1L, "Great framework!", 5, 1L, "testuser", null, null);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frameworkId").value(1))
                .andExpect(jsonPath("$.comment").value("Great framework!"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldPublishReviewCreatedEventToKafka() throws Exception {
        ReviewRequest request = new ReviewRequest(2L, "Solid choice for backend", 4, 2L, "testuser", null, null);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "2")
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
        ReviewRequest request1 = new ReviewRequest(3L, "First review", 3, 3L, "testuser", null, null);
        ReviewRequest request2 = new ReviewRequest(3L, "Second review", 4, 3L, "testuser", null, null);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "3")
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        Thread.sleep(10);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "3")
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/framework/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].comment").value("Second review"))
                .andExpect(jsonPath("$.content[1].comment").value("First review"));
    }

    @Test
    void shouldReturnEmptyListWhenNoReviewsForFramework() throws Exception {
        mockMvc.perform(get("/api/reviews/framework/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldRejectInvalidReviewRequest() throws Exception {
        ReviewRequest invalidRequest = new ReviewRequest(null, "", 6, 1L, "testuser", null, null);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHideReviewWhenAdmin() throws Exception {
        ReviewRequest request = new ReviewRequest(10L, "To be hidden", 3, 10L, "testuser", null, null);

        String response = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "10")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reviewId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/reviews/{id}/moderate", reviewId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hidden\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(true));
    }

    @Test
    void shouldRejectModerationWhenNotAdmin() throws Exception {
        ReviewRequest request = new ReviewRequest(11L, "Normal review", 4, 11L, "testuser", null, null);

        String response = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "11")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reviewId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/reviews/{id}/moderate", reviewId)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hidden\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldExcludeHiddenReviewsFromPublicList() throws Exception {
        ReviewRequest request = new ReviewRequest(12L, "Hidden review", 2, 12L, "testuser", null, null);

        String response = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "12")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reviewId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/reviews/{id}/moderate", reviewId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hidden\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/framework/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturnPaginatedReviewsForFramework() throws Exception {
        for (int i = 0; i < 3; i++) {
            ReviewRequest request = new ReviewRequest(20L, "Review " + i, 4, 20L, "testuser", "Great docs", "Steep learning");
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "20")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/reviews/framework/20?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldCreateReviewWithProsAndCons() throws Exception {
        ReviewRequest request = new ReviewRequest(21L, "Detailed review", 5, 21L, "testuser", "Fast and reliable", "Limited ecosystem");
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "21")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pros").value("Fast and reliable"))
                .andExpect(jsonPath("$.cons").value("Limited ecosystem"))
                .andExpect(jsonPath("$.helpfulVotes").value(0))
                .andExpect(jsonPath("$.notHelpfulVotes").value(0));
    }

    @Test
    void shouldVoteReviewAsHelpful() throws Exception {
        ReviewRequest request = new ReviewRequest(22L, "Vote target", 3, 22L, "testuser", null, null);
        String response = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "22")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reviewId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/reviews/{id}/vote", reviewId)
                        .header("X-User-Id", "22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\": \"UPVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulVotes").value(1))
                .andExpect(jsonPath("$.notHelpfulVotes").value(0));
    }

    @Test
    void shouldToggleOffVoteWhenSameVoteTypeSubmittedAgain() throws Exception {
        ReviewRequest request = new ReviewRequest(23L, "Toggle target", 3, 23L, "testuser", null, null);
        String response = mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "23")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reviewId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/reviews/{id}/vote", reviewId)
                        .header("X-User-Id", "23")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\": \"UPVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulVotes").value(1));

        mockMvc.perform(post("/api/reviews/{id}/vote", reviewId)
                        .header("X-User-Id", "23")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\": \"UPVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulVotes").value(0));
    }

    @Test
    void shouldRejectVoteWithoutUserId() throws Exception {
        mockMvc.perform(post("/api/reviews/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\": \"UPVOTE\"}"))
                .andExpect(status().isUnauthorized());
    }
}
