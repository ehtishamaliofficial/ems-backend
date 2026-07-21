package com.nadra.ems.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nadra.ems.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT authentication filter — intercepts every request, extracts and validates
 * the JWT from the Authorization header, and sets the Spring Security context.
 * <p>
 * Also enforces <strong>scope-based access control</strong> for scoped tokens:
 * <ul>
 *     <li>{@code 2FA_SETUP} scope — only allows {@code /api/v1/auth/2fa/setup} and {@code /api/v1/auth/2fa/enable}</li>
 *     <li>{@code 2FA_VERIFY} scope — only allows {@code /api/v1/auth/verify-2fa}</li>
 * </ul>
 * Full access tokens (no scope) pass through to normal authorization rules.
 * <p>
 * Also enriches MDC with {@code userId} and {@code erpNo} for structured logging.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Allowed endpoints for each token scope */
    private static final Set<String> SCOPE_2FA_SETUP_PATHS = Set.of(
            "/api/v1/auth/2fa/setup",
            "/api/v1/auth/2fa/enable"
    );
    private static final Set<String> SCOPE_2FA_VERIFY_PATHS = Set.of(
            "/api/v1/auth/verify-2fa"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = extractJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            try {
                // Check if this is a scoped token
                String scope = jwtTokenProvider.getTokenScope(jwt);
                if (scope != null) {
                    // Enforce scope-based access control
                    if (!isScopeAllowedForPath(scope, request.getRequestURI())) {
                        log.warn("Scoped token denied: scope={}, uri={}", scope, request.getRequestURI());
                        writeForbiddenResponse(response, request.getRequestURI(),
                                "This token is restricted to " + scope + " operations only.");
                        return;
                    }
                }

                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // Enrich MDC for structured logging
                MDC.put("userId", userId.toString());

                UserDetails userDetails;
                if (scope != null) {
                    // Scoped tokens don't carry username/erpNo — load by userId
                    userDetails = userDetailsService.loadUserById(userId);
                } else {
                    // Full access tokens carry all claims
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    String erpNo = jwtTokenProvider.getErpNoFromToken(jwt);
                    MDC.put("erpNo", erpNo != null ? erpNo : "");
                    userDetails = userDetailsService.loadUserByUsername(username);
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: userId={}, scope={}", userId, scope);

            } catch (Exception ex) {
                log.warn("Could not set user authentication from JWT: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks whether the given scope allows access to the given request path.
     */
    private boolean isScopeAllowedForPath(String scope, String requestUri) {
        return switch (scope) {
            case "2FA_SETUP" -> SCOPE_2FA_SETUP_PATHS.contains(requestUri);
            case "2FA_VERIFY" -> SCOPE_2FA_VERIFY_PATHS.contains(requestUri);
            default -> false; // Unknown scope — deny
        };
    }

    /**
     * Writes a 403 Forbidden JSON response for scope violations.
     */
    private void writeForbiddenResponse(HttpServletResponse response, String path, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        ApiResponse<Void> apiResponse = ApiResponse.error(403, message, path);
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }

    /**
     * Extracts JWT token from the "Authorization: Bearer &lt;token&gt;" header.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
