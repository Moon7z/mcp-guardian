package com.guardian.policy.config;

import com.guardian.policy.model.PolicyConfig;
import com.guardian.policy.model.PolicyRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "com.guardian.policy")
public class PolicyAutoConfiguration {

    @Bean
    public PolicyConfig policyConfig(PolicyProperties properties) {
        List<PolicyRule> rules = List.of();
        PolicyConfig.DefaultAction defaultAction = PolicyConfig.DefaultAction.ALLOW;

        if (properties.getRules() != null) {
            rules = properties.getRules().stream()
                    .map(r -> new PolicyRule(
                            r.getName(), r.getDescription(),
                            PolicyRule.Action.valueOf(r.getAction()),
                            r.getMethods(), r.getKeywords(), r.getRoles()))
                    .toList();
        }

        if (properties.getDefaultAction() != null) {
            defaultAction = PolicyConfig.DefaultAction.valueOf(properties.getDefaultAction());
        }

        return new PolicyConfig(rules, defaultAction);
    }
}
