package com.guardian.policy.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.guardian.policy.model.PolicyConfig;
import com.guardian.policy.model.PolicyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PolicyEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);
    private final PolicyConfig policyConfig;

    public PolicyEvaluator(PolicyConfig policyConfig) {
        this.policyConfig = policyConfig;
    }

    public record EvaluationResult(boolean allowed, String ruleName, String reason) {
        public static EvaluationResult allow() {
            return new EvaluationResult(true, null, null);
        }

        public static EvaluationResult deny(String ruleName, String reason) {
            return new EvaluationResult(false, ruleName, reason);
        }
    }

    public EvaluationResult evaluate(String method, JsonNode params, String userRole) {
        if (policyConfig.rules() == null || policyConfig.rules().isEmpty()) {
            return defaultResult();
        }

        for (PolicyRule rule : policyConfig.rules()) {
            if (matchesRule(rule, method, params, userRole)) {
                if (rule.action() == PolicyRule.Action.DENY) {
                    log.warn("Policy DENY: rule={}, method={}, user_role={}", rule.name(), method, userRole);
                    return EvaluationResult.deny(rule.name(), rule.description());
                }
            }
        }

        return defaultResult();
    }

    private boolean matchesRule(PolicyRule rule, String method, JsonNode params, String userRole) {
        // Check method match
        if (rule.methods() != null && !rule.methods().isEmpty()) {
            boolean methodMatch = rule.methods().stream()
                    .anyMatch(m -> method != null && method.matches(m));
            if (!methodMatch) {
                return false;
            }
        }

        // Check role exclusion (rule applies when user does NOT have required role)
        if (rule.roles() != null && !rule.roles().isEmpty()) {
            if (userRole != null && rule.roles().contains(userRole)) {
                return false; // user has the required role, rule doesn't apply
            }
        }

        // Check keyword match in params
        if (rule.keywords() != null && !rule.keywords().isEmpty()) {
            String paramsText = params != null ? params.toString().toLowerCase() : "";
            boolean keywordMatch = rule.keywords().stream()
                    .anyMatch(kw -> paramsText.contains(kw.toLowerCase()));
            if (!keywordMatch) {
                return false;
            }
        }

        return true;
    }

    private EvaluationResult defaultResult() {
        if (policyConfig.defaultAction() == PolicyConfig.DefaultAction.DENY) {
            return EvaluationResult.deny("default", "Default policy is DENY");
        }
        return EvaluationResult.allow();
    }
}
