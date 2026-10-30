package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gateway's routing table must cover every connector of both business services and target them by
 * service name (lb://) so routing goes through Eureka rather than hardcoded hosts.
 */
@SpringBootTest
class ApiGatewayRoutesTest {

    @Autowired
    GatewayMvcProperties properties;

    @Test
    void everyConnectorIsRoutedThroughDiscovery() {
        List<String> ids = properties.getRoutes().stream().map(r -> r.getId()).toList();

        assertThat(ids).containsExactlyInAnyOrder("account-rest", "transaction-rest", "account-graphql",
                "transaction-graphql", "account-mcp", "transaction-mcp");
        assertThat(properties.getRoutes())
                .allSatisfy(route -> assertThat(route.getUri().getScheme()).isEqualTo("lb"));
    }

    @Test
    void restRoutesPointAtTheOwningService() {
        assertThat(routeUri("account-rest")).isEqualTo("lb://account-service");
        assertThat(routeUri("transaction-rest")).isEqualTo("lb://transaction-service");
    }

    private String routeUri(String id) {
        return properties.getRoutes().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no route " + id))
                .getUri().toString();
    }
}
