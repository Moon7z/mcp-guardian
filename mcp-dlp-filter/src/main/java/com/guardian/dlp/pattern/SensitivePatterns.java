package com.guardian.dlp.pattern;

import java.util.List;
import java.util.regex.Pattern;

public final class SensitivePatterns {

    private SensitivePatterns() {}

    public record SensitivePattern(String name, Pattern pattern, String replacement) {}

    public static final String REDACTED = "[REDACTED_BY_GUARDIAN]";

    public static final List<SensitivePattern> ALL_PATTERNS = List.of(
            // Chinese mobile phone numbers: 1[3-9]X-XXXX-XXXX
            new SensitivePattern("PHONE_CN",
                    Pattern.compile("(?<![\\d])1[3-9]\\d{9}(?![\\d])"),
                    REDACTED),

            // Chinese ID card numbers (18 digits, last may be X)
            new SensitivePattern("ID_CARD_CN",
                    Pattern.compile("(?<![\\d])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?![\\d])"),
                    REDACTED),

            // Email addresses
            new SensitivePattern("EMAIL",
                    Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
                    REDACTED),

            // API keys (generic patterns: sk-xxx, ak-xxx, key-xxx, token-xxx)
            new SensitivePattern("API_KEY",
                    Pattern.compile("(?i)(?:sk|ak|key|token|secret|password|apikey|api_key|access_key|secret_key)-[a-zA-Z0-9_-]{16,}"),
                    REDACTED),

            // AWS-style access keys
            new SensitivePattern("AWS_KEY",
                    Pattern.compile("(?:AKIA|ABIA|ACCA)[A-Z0-9]{16}"),
                    REDACTED),

            // Generic passwords in config strings: password=xxx, passwd=xxx, pwd=xxx
            new SensitivePattern("PASSWORD_CONFIG",
                    Pattern.compile("(?i)(?:password|passwd|pwd|secret)\\s*[=:]\\s*['\"]?([^\\s'\"}{,;]+)"),
                    REDACTED),

            // Database connection strings with passwords
            new SensitivePattern("DB_CONN_STRING",
                    Pattern.compile("(?i)(?:jdbc|mysql|postgresql|mongodb|redis)://[^\\s]+:[^@\\s]+@"),
                    REDACTED),

            // Credit card numbers (basic Luhn-agnostic pattern)
            new SensitivePattern("CREDIT_CARD",
                    Pattern.compile("(?<![\\d])(?:4\\d{3}|5[1-5]\\d{2}|6(?:011|5\\d{2})|3[47]\\d{2})[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}(?![\\d])"),
                    REDACTED),

            // IPv4 private addresses (for internal infra masking)
            new SensitivePattern("PRIVATE_IP",
                    Pattern.compile("(?<![\\d])(?:10\\.\\d{1,3}|172\\.(?:1[6-9]|2\\d|3[01])|192\\.168)\\.\\d{1,3}\\.\\d{1,3}(?![\\d])"),
                    REDACTED),

            // Bearer tokens in headers
            new SensitivePattern("BEARER_TOKEN",
                    Pattern.compile("(?i)Bearer\\s+[a-zA-Z0-9_\\-\\.]+"),
                    "Bearer " + REDACTED)
    );
}
