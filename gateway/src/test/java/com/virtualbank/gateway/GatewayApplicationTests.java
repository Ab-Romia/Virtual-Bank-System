package com.virtualbank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full gateway context to prove the wiring loads: routes, the reactive
 * security filter chain, CORS, the WebClient, and the dashboard aggregation
 * controller. The test JWKS uri is a dummy (see src/test/resources/application.yml);
 * the reactive JWT decoder is lazy, so user-service does not need to be running.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class GatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
