package com.socialnetwork.auth.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting por IP en los endpoints sensibles de auth (fuerza bruta, spam de emails,
 * enumeración). Contador simple INCR+EXPIRE en Redis: suficiente para el MVP; si hiciera falta
 * algo más fino (token bucket, por usuario), migrar a Bucket4j detrás de un puerto.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(int limit, Duration window) {}

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, Duration.ofMinutes(1)),
            "/api/auth/register", new Rule(5, Duration.ofMinutes(15)),
            "/api/auth/resend-verification", new Rule(3, Duration.ofMinutes(15)),
            "/api/auth/verify-email", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/forgot-password", new Rule(3, Duration.ofMinutes(15)),
            "/api/auth/reset-password", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/login/mfa", new Rule(10, Duration.ofMinutes(5)));

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = "POST".equals(request.getMethod()) ? RULES.get(request.getRequestURI()) : null;
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = "auth:rl:" + request.getRequestURI() + ":" + clientIp(request);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, rule.window());
        }
        if (count != null && count > rule.limit()) {
            reject(response, rule);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, Rule rule) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Demasiadas peticiones. Inténtalo de nuevo más tarde.");
        problem.setType(URI.create("https://socialnetwork.dev/problems/rate_limited"));
        problem.setProperty("code", "rate_limited");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    /** Primera IP de X-Forwarded-For (NGINX delante en producción) o la remota directa. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
