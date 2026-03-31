package com.guardian.policy.model;

import java.util.List;

public record PolicyConfig(
        List<PolicyRule> rules,
        DefaultAction defaultAction
) {
    public enum DefaultAction {
        ALLOW, DENY
    }
}
