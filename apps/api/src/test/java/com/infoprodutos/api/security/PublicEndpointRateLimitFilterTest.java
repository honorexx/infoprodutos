package com.infoprodutos.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicEndpointRateLimitFilterTest {

    @Test
    void blocksLoginAfterConfiguredLimitForSameClient() throws Exception {
        var filter = new PublicEndpointRateLimitFilter(new ObjectMapper());

        for (int i = 0; i < 20; i++) {
            var response = invoke(filter, "/api/v1/auth/login", "203.0.113.10");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        var blocked = invoke(filter, "/api/v1/auth/login", "203.0.113.10");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("rate-limit-exceeded");
    }

    @Test
    void keepsIndependentWindowsForDifferentClients() throws Exception {
        var filter = new PublicEndpointRateLimitFilter(new ObjectMapper());
        for (int i = 0; i < 20; i++) {
            invoke(filter, "/api/v1/auth/login", "203.0.113.10");
        }

        var otherClient = invoke(filter, "/api/v1/auth/login", "203.0.113.11");
        assertThat(otherClient.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletResponse invoke(
            PublicEndpointRateLimitFilter filter, String path, String remoteAddress) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddress);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
