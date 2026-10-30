package com.example.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Eureka registry must start and serve its registration API. A real port is required: the registry is
 * published by a Jersey servlet, which MockMvc does not dispatch to.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServiceApplicationTest {

    @Value("${local.server.port}")
    int port;

    private ResponseEntity<String> get(String path) {
        return RestClient.create().get().uri("http://localhost:" + port + path)
                .accept(MediaType.APPLICATION_JSON).retrieve().toEntity(String.class);
    }

    @Test
    void registryIsUpAndAcceptsApplicationQueries() {
        ResponseEntity<String> response = get("/eureka/apps");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void healthEndpointIsUp() {
        assertThat(get("/actuator/health").getBody()).contains("UP");
    }
}
