package com.guardian.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guardian.policy.engine.PolicyEvaluator;
import com.guardian.policy.model.PolicyConfig;
import com.guardian.policy.model.PolicyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEvaluatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PolicyConfig configWith(PolicyRule... rules) {
        return new PolicyConfig(List.of(rules), PolicyConfig.DefaultAction.ALLOW);
    }

    @Test
    @DisplayName("Should allow request when no rules defined")
    void allowWhenNoRules() {
        var evaluator = new PolicyEvaluator(new PolicyConfig(List.of(), PolicyConfig.DefaultAction.ALLOW));
        var result = evaluator.evaluate("tools/call", null, "user");
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should block DROP keyword for non-admin users")
    void blockDropForNonAdmin() {
        var rule = new PolicyRule("block-sql", "Block destructive SQL",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("drop"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE users");

        var result = evaluator.evaluate("tools/call", params, "developer");
        assertFalse(result.allowed());
        assertEquals("block-sql", result.ruleName());
    }

    @Test
    @DisplayName("Should allow DROP keyword for admin users (exempt role)")
    void allowDropForAdmin() {
        var rule = new PolicyRule("block-sql", "Block destructive SQL",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("drop"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE temp");

        var result = evaluator.evaluate("tools/call", params, "admin");
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should block TRUNCATE keyword")
    void blockTruncate() {
        var rule = new PolicyRule("block-truncate", "Block truncate",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("truncate"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "TRUNCATE TABLE logs");

        var result = evaluator.evaluate("tools/call", params, "analyst");
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Should not match when method doesn't match")
    void noMatchWrongMethod() {
        var rule = new PolicyRule("block-sql", "Block SQL",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("drop"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE users");

        var result = evaluator.evaluate("resources/read", params, "developer");
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should use default DENY when configured")
    void defaultDeny() {
        var config = new PolicyConfig(List.of(), PolicyConfig.DefaultAction.DENY);
        var evaluator = new PolicyEvaluator(config);
        var result = evaluator.evaluate("tools/call", null, "user");
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Should handle case-insensitive keyword matching")
    void caseInsensitiveKeywords() {
        var rule = new PolicyRule("block-delete", "Block delete",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("delete from"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DELETE FROM users WHERE id = 1");

        var result = evaluator.evaluate("tools/call", params, "developer");
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Should use equals() not regex for method matching")
    void methodMatchUsesEqualsNotRegex() {
        // "tools/call" should NOT be treated as regex — test with regex metacharacters
        var rule = new PolicyRule("test-rule", "Test",
                PolicyRule.Action.DENY,
                List.of("tools/call[v2]"), List.of("drop"), List.of());

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE users");

        // Exact match should fail: method is "tools/callv2" which does not equal "tools/call[v2]"
        var result = evaluator.evaluate("tools/callv2", params, "user");
        assertTrue(result.allowed());

        // Exact match should succeed
        var result2 = evaluator.evaluate("tools/call[v2]", params, "user");
        assertFalse(result2.allowed());
    }

    @Test
    @DisplayName("Should handle null method gracefully")
    void nullMethodDoesNotMatch() {
        var rule = new PolicyRule("block-sql", "Block SQL",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("drop"), List.of("admin"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE users");

        var result = evaluator.evaluate(null, params, "developer");
        assertTrue(result.allowed()); // null method won't match "tools/call"
    }

    @Test
    @DisplayName("Should handle multiple exempt roles")
    void multipleExemptRoles() {
        var rule = new PolicyRule("block-sql", "Block SQL",
                PolicyRule.Action.DENY,
                List.of("tools/call"), List.of("drop"), List.of("admin", "dba"));

        var evaluator = new PolicyEvaluator(configWith(rule));
        ObjectNode params = objectMapper.createObjectNode();
        params.put("query", "DROP TABLE users");

        assertTrue(evaluator.evaluate("tools/call", params, "admin").allowed());
        assertTrue(evaluator.evaluate("tools/call", params, "dba").allowed());
        assertFalse(evaluator.evaluate("tools/call", params, "developer").allowed());
    }
}
