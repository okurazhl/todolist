package com.smartmemo.gateway.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnHealthStatus() {
        webTestClient.get()
                .uri("/api/v1/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.data.service").isEqualTo("api-gateway")
                .jsonPath("$.data.status").isEqualTo("UP")
                .jsonPath("$.traceId").exists();
    }

    @Test
    void shouldReturnUpViaActuator() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
