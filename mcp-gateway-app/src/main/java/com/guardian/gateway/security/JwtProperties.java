package com.guardian.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guardian.jwt")
public class JwtProperties {

    private String secret = "change-this-secret-key-in-production-must-be-at-least-32-bytes";
    private long expirationMs = 86400000; // 24 hours
    private String header = "Authorization";
    private String prefix = "Bearer ";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
}
