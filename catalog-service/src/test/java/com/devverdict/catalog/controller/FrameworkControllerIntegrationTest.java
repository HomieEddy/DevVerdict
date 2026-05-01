package com.devverdict.catalog.controller;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.repository.FrameworkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FrameworkControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @BeforeEach
    void setUp() {
        frameworkRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoFrameworks() throws Exception {
        mockMvc.perform(get("/api/catalog/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnAllFrameworks() throws Exception {
        frameworkRepository.save(new Framework("Spring Boot", "Framework", "Java framework", 4.5));
        frameworkRepository.save(new Framework("React", "Framework", "JS library", 4.5));

        mockMvc.perform(get("/api/catalog/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Spring Boot")))
                .andExpect(jsonPath("$[1].name", is("React")));
    }

    @Test
    void shouldReturnFrameworkById() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("Angular", "Framework", "Google framework", 4.2));

        mockMvc.perform(get("/api/catalog/frameworks/{id}", framework.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Angular")))
                .andExpect(jsonPath("$.type", is("Framework")))
                .andExpect(jsonPath("$.description", is("Google framework")))
                .andExpect(jsonPath("$.averageRating", is(4.2)));
    }

    @Test
    void shouldReturn404WhenFrameworkNotFound() throws Exception {
        mockMvc.perform(get("/api/catalog/frameworks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateFrameworkWhenAdmin() throws Exception {
        mockMvc.perform(post("/api/catalog/frameworks")
                        .header("X-User-Role", "ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"name":"Vue","type":"Framework","description":"Progressive JS framework","averageRating":4.3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Vue")))
                .andExpect(jsonPath("$.type", is("Framework")))
                .andExpect(jsonPath("$.averageRating", is(4.3)));
    }

    @Test
    void shouldRejectFrameworkCreationWhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/catalog/frameworks")
                        .header("X-User-Role", "USER")
                        .contentType("application/json")
                        .content("""
                                {"name":"Vue","type":"Framework","description":"Progressive JS framework","averageRating":4.3}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateFrameworkWhenAdmin() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("Old", "Framework", "Old desc", 3.0));

        mockMvc.perform(put("/api/catalog/frameworks/{id}", framework.getId())
                        .header("X-User-Role", "ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"name":"Updated","type":"Library","description":"Updated desc","averageRating":4.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated")))
                .andExpect(jsonPath("$.type", is("Library")))
                .andExpect(jsonPath("$.averageRating", is(4.0)));
    }

    @Test
    void shouldRejectFrameworkUpdateWhenNotAdmin() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("Old", "Framework", "Old desc", 3.0));

        mockMvc.perform(put("/api/catalog/frameworks/{id}", framework.getId())
                        .header("X-User-Role", "USER")
                        .contentType("application/json")
                        .content("""
                                {"name":"Updated","type":"Library","description":"Updated desc","averageRating":4.0}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteFrameworkWhenAdmin() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("DeleteMe", "Framework", "Desc", 3.0));

        mockMvc.perform(delete("/api/catalog/frameworks/{id}", framework.getId())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectFrameworkDeleteWhenNotAdmin() throws Exception {
        Framework framework = frameworkRepository.save(new Framework("DeleteMe", "Framework", "Desc", 3.0));

        mockMvc.perform(delete("/api/catalog/frameworks/{id}", framework.getId())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnConflictWhenDeletingFrameworkWithReviews() throws Exception {
        Framework framework = new Framework("WithReviews", "Framework", "Desc", 3.0);
        framework.setReviewCount(5);
        framework = frameworkRepository.save(framework);

        mockMvc.perform(delete("/api/catalog/frameworks/{id}", framework.getId())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isConflict());
    }
}
