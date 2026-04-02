package com.guardian.policy.config;

import com.guardian.policy.model.PolicyConfig;
import com.guardian.policy.model.PolicyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "com.guardian.policy")
@EnableConfigurationProperties(PolicyProperties.class)
public class PolicyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PolicyAutoConfiguration.class);

    @Bean
    public PolicyConfig policyConfig(PolicyProperties properties) {
        List<PolicyRule> rules = List.of();
        PolicyConfig.DefaultAction defaultAction = PolicyConfig.DefaultAction.ALLOW;

        if (properties.getRules() != null) {
            rules = properties.getRules().stream()
                    .map(this::toRule)
                    .toList();
        }

        if (properties.getDefaultAction() != null) {
            try {
                defaultAction = PolicyConfig.DefaultAction.valueOf(
                        properties.getDefaultAction().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid policy default-action '{}', falling back to ALLOW. " +
                        "Valid values: ALLOW, DENY", properties.getDefaultAction());
            }
        }

        return new PolicyConfig(rules, defaultAction);
    }

    private PolicyRule toRule(PolicyProperties.RuleProperties r) {
        PolicyRule.Action action;
        if (r.getAction() == null) {
            log.warn("Missing action in rule '{}', defaulting to DENY", r.getName());
            action = PolicyRule.Action.DENY;
        } else {
            try {
                action = PolicyRule.Action.valueOf(r.getAction().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid action '{}' in rule '{}', defaulting to DENY. " +
                        "Valid values: ALLOW, DENY", r.getAction(), r.getName());
                action = PolicyRule.Action.DENY;
            }
        }
        return new PolicyRule(
                r.getName(), r.getDescription(), action,
                r.getMethods() != null ? r.getMethods() : List.of(),
                r.getKeywords() != null ? r.getKeywords() : List.of(),
                r.getExemptRoles() != null ? r.getExemptRoles() : List.of());
    }
}
