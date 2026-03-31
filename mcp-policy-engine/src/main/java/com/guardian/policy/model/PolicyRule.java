package com.guardian.policy.model;

import java.util.List;

public record PolicyRule(
        String name,
        String description,
        Action action,
        List<String> methods,
        List<String> keywords,
        List<String> exemptRoles
) {
    public enum Action {
        DENY, ALLOW
    }
}
