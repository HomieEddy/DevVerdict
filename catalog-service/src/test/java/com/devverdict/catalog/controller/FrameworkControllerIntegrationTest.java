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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
