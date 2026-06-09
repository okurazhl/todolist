package com.smartmemo.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * AuthFilter 集成测试。
 * 需要完整 Spring 上下文才能加载 Gateway 过滤器链。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowPublicPaths() {
        webTestClient.get()
                .uri("/api/v1/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_UNAUTHORIZED");
    }

    @Test
    void shouldReturn401WithInvalidToken() {
        webTestClient.get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer invalid-token-blah-blah")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_TOKEN_INVALID");
    }
}
