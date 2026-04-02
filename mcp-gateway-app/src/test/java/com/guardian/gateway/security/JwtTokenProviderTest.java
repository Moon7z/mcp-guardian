package com.guardian.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        var props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(3600000); // 1 hour
        provider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("Should generate a valid token")
    void generateToken() {
        String token = provider.generateToken("alice", "session-1", List.of("USER", "ADMIN"));
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should validate a valid token")
    void validateValidToken() {
        String token = provider.generateToken("alice", "session-1", List.of("USER"));
        assertTrue(provider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void rejectExpiredToken() {
        // Build a token that's already expired using JJWT directly
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("alice")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertFalse(provider.validateToken(expired));
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void rejectInvalidSignature() {
        var wrongKey = Keys.hmacShaKeyFor(
                "different-secret-key-also-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        assertFalse(provider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject malformed token")
    void rejectMalformedToken() {
        assertFalse(provider.validateToken("not.a.jwt"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken("random-garbage"));
    }

    @Test
    @DisplayName("Should extract userId from token")
    void extractUserId() {
        String token = provider.generateToken("bob", "session-2", List.of("USER"));
        assertEquals("bob", provider.getUserId(token));
    }

    @Test
    @DisplayName("Should extract sessionId from token")
    void extractSessionId() {
        String token = provider.generateToken("bob", "session-2", List.of("USER"));
        assertEquals("session-2", provider.getSessionId(token));
    }

    @Test
    @DisplayName("Should extract roles from token")
    void extractRoles() {
        String token = provider.generateToken("alice", "s1", List.of("USER", "ADMIN"));
        List<String> roles = provider.getRoles(token);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    @DisplayName("Should handle null roles gracefully")
    void nullRolesDefaultsToEmpty() {
        String token = provider.generateToken("alice", "s1", null);
        List<String> roles = provider.getRoles(token);
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("Should reject secret shorter than 32 bytes")
    void rejectShortSecret() {
        var props = new JwtProperties();
        props.setSecret("too-short");
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(props));
    }
}
