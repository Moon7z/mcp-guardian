package com.guardian.gateway.security;

import com.guardian.core.transport.McpProxyController;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties properties;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, JwtProperties properties) {
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = tokenProvider.parseClaimsFromToken(token);
                String userId = claims.getSubject();
                String sessionId = claims.get("sessionId", String.class);
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles") instanceof List<?> list
                        ? (List<String>) list : List.of();

                var authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .toList();

                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                String primaryRole = roles.isEmpty() ? null : roles.get(0).toLowerCase();
                authentication.setDetails(new JwtAuthDetails(userId, sessionId, roles, primaryRole));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user: {}, roles: {}, session: {}", userId, roles, sessionId);
            } catch (Exception e) {
                log.debug("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(properties.getHeader());
        if (header != null && header.startsWith(properties.getPrefix())) {
            return header.substring(properties.getPrefix().length()).trim();
        }
        return null;
    }

    public record JwtAuthDetails(
            String userId,
            String sessionId,
            List<String> roles,
            String userRole
    ) implements McpProxyController.AuthDetails {}
}
