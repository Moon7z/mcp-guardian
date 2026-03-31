package com.guardian.dlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.dlp.filter.DlpRedactor;
import com.guardian.dlp.pattern.SensitivePatterns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DlpRedactorTest {

    private DlpRedactor redactor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redactor = new DlpRedactor(objectMapper);
    }

    @Test
    @DisplayName("Should redact Chinese phone numbers")
    void redactChinesePhoneNumber() {
        var result = redactor.redactText("Call me at 13812345678 please");
        assertEquals("Call me at [REDACTED_BY_GUARDIAN] please", result.redactedText());
        assertTrue(result.detectedTypes().contains("PHONE_CN"));
    }

    @Test
    @DisplayName("Should redact Chinese ID card numbers")
    void redactChineseIdCard() {
        var result = redactor.redactText("ID: 110101199003071234");
        assertEquals("ID: [REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("ID_CARD_CN"));
    }

    @Test
    @DisplayName("Should redact ID card with X suffix")
    void redactIdCardWithX() {
        var result = redactor.redactText("ID: 11010119900307123X");
        assertEquals("ID: [REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("ID_CARD_CN"));
    }

    @Test
    @DisplayName("Should redact email addresses")
    void redactEmail() {
        var result = redactor.redactText("Send to user@example.com for info");
        assertEquals("Send to [REDACTED_BY_GUARDIAN] for info", result.redactedText());
        assertTrue(result.detectedTypes().contains("EMAIL"));
    }

    @Test
    @DisplayName("Should redact API keys")
    void redactApiKey() {
        var result = redactor.redactText("Use sk-abc123def456ghi789jkl012");
        assertEquals("Use [REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("API_KEY"));
    }

    @Test
    @DisplayName("Should redact AWS access keys")
    void redactAwsKey() {
        var result = redactor.redactText("Key: AKIAIOSFODNN7EXAMPLE");
        assertEquals("Key: [REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("AWS_KEY"));
    }

    @Test
    @DisplayName("Should redact password in config strings, preserving key name")
    void redactPasswordConfig() {
        var result = redactor.redactText("password=MyS3cretP@ss");
        assertEquals("password=[REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("PASSWORD_CONFIG"));
    }

    @Test
    @DisplayName("Should redact password with colon separator")
    void redactPasswordWithColon() {
        var result = redactor.redactText("secret: my_secret_value");
        assertEquals("secret: [REDACTED_BY_GUARDIAN]", result.redactedText());
        assertTrue(result.detectedTypes().contains("PASSWORD_CONFIG"));
    }

    @Test
    @DisplayName("Should redact database connection strings, preserving host")
    void redactDbConnectionString() {
        var result = redactor.redactText("jdbc://admin:password123@db.example.com:5432/mydb");
        assertFalse(result.redactedText().contains("password123"));
        assertTrue(result.redactedText().contains("db.example.com"));
        assertTrue(result.redactedText().contains(SensitivePatterns.REDACTED));
    }

    @Test
    @DisplayName("Should redact Bearer tokens")
    void redactBearerToken() {
        var result = redactor.redactText("Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test");
        assertTrue(result.redactedText().contains("[REDACTED_BY_GUARDIAN]"));
        assertTrue(result.detectedTypes().contains("BEARER_TOKEN"));
    }

    @Test
    @DisplayName("Should redact private IP addresses")
    void redactPrivateIp() {
        var result = redactor.redactText("Server at 192.168.1.100 is down");
        assertEquals("Server at [REDACTED_BY_GUARDIAN] is down", result.redactedText());
        assertTrue(result.detectedTypes().contains("PRIVATE_IP"));
    }

    @Test
    @DisplayName("Should handle null input")
    void handleNull() {
        var result = redactor.redactText(null);
        assertNull(result.redactedText());
        assertTrue(result.detectedTypes().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty input")
    void handleEmpty() {
        var result = redactor.redactText("");
        assertEquals("", result.redactedText());
        assertTrue(result.detectedTypes().isEmpty());
    }

    @Test
    @DisplayName("Should not redact clean text")
    void noRedactionForCleanText() {
        var result = redactor.redactText("This is a normal query with no sensitive data");
        assertEquals("This is a normal query with no sensitive data", result.redactedText());
        assertTrue(result.detectedTypes().isEmpty());
    }

    @Test
    @DisplayName("Should redact multiple patterns in one string")
    void redactMultiplePatterns() {
        var result = redactor.redactText("Contact 13812345678, email user@test.com, server at 10.0.1.5");
        assertFalse(result.redactedText().contains("13812345678"));
        assertFalse(result.redactedText().contains("user@test.com"));
        assertFalse(result.redactedText().contains("10.0.1.5"));
        assertTrue(result.detectedTypes().size() >= 3);
    }

    @Test
    @DisplayName("Should redact sensitive data in JSON nodes")
    void redactJsonNode() throws Exception {
        String json = """
                {
                    "name": "test",
                    "phone": "13812345678",
                    "nested": {
                        "email": "user@example.com",
                        "data": ["normal", "10.0.1.5"]
                    }
                }
                """;
        JsonNode node = objectMapper.readTree(json);
        JsonNode redacted = redactor.redactJsonNode(node);

        assertEquals("test", redacted.get("name").asText());
        assertEquals(SensitivePatterns.REDACTED, redacted.get("phone").asText());
        assertEquals(SensitivePatterns.REDACTED, redacted.get("nested").get("email").asText());
        assertEquals(SensitivePatterns.REDACTED, redacted.get("nested").get("data").get(1).asText());
    }

    @Test
    @DisplayName("Should redact JSON string")
    void redactJsonString() {
        String json = "{\"password\":\"password=secret123\"}";
        String redacted = redactor.redactJsonString(json);
        assertFalse(redacted.contains("secret123"));
    }

    @Test
    @DisplayName("Should handle null JSON node")
    void redactNullJsonNode() {
        assertNull(redactor.redactJsonNode(null));
    }
}
