package com.devverdict.gateway.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldHaveCatalogServiceRoute() {
        StepVerifier.create(routeLocator.getRoutes().collectList())
                .assertNext(routes -> {
                    boolean hasCatalogRoute = routes.stream()
                            .anyMatch(route -> route.getId().equals("catalog-service"));
                    org.junit.jupiter.api.Assertions.assertTrue(hasCatalogRoute, "Catalog service route should be defined");
                })
                .verifyComplete();
    }

    @Test
    void shouldHaveReviewServiceRoute() {
        StepVerifier.create(routeLocator.getRoutes().collectList())
                .assertNext(routes -> {
                    boolean hasReviewRoute = routes.stream()
                            .anyMatch(route -> route.getId().equals("review-service"));
                    org.junit.jupiter.api.Assertions.assertTrue(hasReviewRoute, "Review service route should be defined");
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn404ForUnmatchedPath() {
        webTestClient.get()
                .uri("/nonexistent/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldHandleCorsPreflight() {
        webTestClient.options()
                .uri("/api/catalog/frameworks")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200")
                .expectHeader().valueEquals("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    }
}
