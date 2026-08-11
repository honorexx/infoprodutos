package com.infoprodutos.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Proteção local e simples contra abuso dos endpoints públicos de autenticação.
 * O limite é por IP resolvido pelo container; em produção o proxy deve sobrescrever
 * os headers encaminhados e {@code server.forward-headers-strategy=native} deve estar ativo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Policy> POLICIES = Map.of(
            "/api/v1/auth/login", new Policy(20, Duration.ofMinutes(1)),
            "/api/v1/auth/register", new Policy(10, Duration.ofHours(1)),
            "/api/v1/auth/password/forgot", new Policy(5, Duration.ofMinutes(15)),
            "/api/v1/auth/password/reset", new Policy(10, Duration.ofMinutes(15)),
            "/api/v1/auth/refresh", new Policy(60, Duration.ofMinutes(1)));

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final ObjectMapper objectMapper;

    public PublicEndpointRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !POLICIES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        Policy policy = POLICIES.get(request.getRequestURI());
        String client = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        String key = request.getRequestURI() + '|' + client;
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        Decision decision = window.acquire(now, policy);

        if (requestCounter.incrementAndGet() % 1_000 == 0) {
            windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now, POLICIES.get(
                    entry.getKey().substring(0, entry.getKey().indexOf('|')))));
        }

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            objectMapper.writeValue(
                    response.getWriter(),
                    Map.of(
                            "status", 429,
                            "title", "Muitas tentativas",
                            "detail", "Muitas tentativas. Aguarde antes de tentar novamente.",
                            "code", "rate-limit-exceeded"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Policy(int limit, Duration duration) {}

    private record Decision(boolean allowed, long retryAfterSeconds) {}

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }

        private synchronized Decision acquire(long now, Policy policy) {
            long durationMillis = policy.duration().toMillis();
            if (now - startedAt >= durationMillis) {
                startedAt = now;
                count = 0;
            }
            count++;
            if (count <= policy.limit()) {
                return new Decision(true, 0);
            }
            long remainingMillis = Math.max(1, durationMillis - (now - startedAt));
            return new Decision(false, Math.max(1, (remainingMillis + 999) / 1_000));
        }

        private synchronized boolean isExpired(long now, Policy policy) {
            return policy != null && now - startedAt >= policy.duration().toMillis();
        }
    }
}
