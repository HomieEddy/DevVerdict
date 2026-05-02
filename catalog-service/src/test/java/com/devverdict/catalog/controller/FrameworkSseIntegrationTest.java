package com.devverdict.catalog.controller;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.dto.FrameworkRatingEvent;
import com.devverdict.catalog.repository.FrameworkRepository;
import com.devverdict.catalog.service.SseBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FrameworkSseIntegrationTest {

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

    @LocalServerPort
    int port;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private SseBroadcastService sseBroadcastService;

    @BeforeEach
    void setUp() {
        frameworkRepository.deleteAll();
    }

    @Test
    void shouldStreamRatingUpdatesViaSse() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("Test", "Framework", "Desc", 4.0));
        Long frameworkId = framework.getId();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/catalog/frameworks/" + frameworkId + "/stream"))
                .GET()
                .build();

        CompletableFuture<HttpResponse<java.io.InputStream>> responseFuture =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> linesFuture = executor.submit(() -> {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseFuture.get(5, TimeUnit.SECONDS).body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    if (lines.stream().anyMatch(l -> l.contains("REVIEW_CREATED"))) {
                        break;
                    }
                }
            }
            return lines;
        });

        Thread.sleep(300);
        FrameworkRatingEvent event = new FrameworkRatingEvent("REVIEW_CREATED", frameworkId, 4.5, 1);
        sseBroadcastService.broadcast(frameworkId, event);

        List<String> lines = linesFuture.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(lines)
                .anyMatch(l -> l.contains("event:rating-update"))
                .anyMatch(l -> l.contains("data:{"))
                .anyMatch(l -> l.contains("REVIEW_CREATED"));
    }
}
