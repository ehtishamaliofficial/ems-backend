package com.nadra.ems.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that enriches every request with MDC context for structured logging.
 * <p>
 * MDC fields set:
 * <ul>
 *     <li>{@code requestId} — unique UUID per request (for tracing)</li>
 *     <li>{@code clientIp} — client IP address</li>
 *     <li>{@code method} — HTTP method (GET, POST, etc.)</li>
 *     <li>{@code uri} — request URI</li>
 * </ul>
 * <p>
 * Also logs request/response timing for performance monitoring in Grafana.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        try {
            // Set MDC context for structured logging
            MDC.put("requestId", requestId);
            MDC.put("clientIp", getClientIp(request));
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            log.info("→ {} {} from {}", request.getMethod(), request.getRequestURI(), getClientIp(request));

            // Add request ID to response header for client-side correlation
            response.setHeader("X-Request-Id", requestId);

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("← {} {} — {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

            // Clean up MDC to prevent context leakage between requests
            MDC.clear();
        }
    }

    /**
     * Extracts the real client IP, respecting X-Forwarded-For headers from proxies/load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
