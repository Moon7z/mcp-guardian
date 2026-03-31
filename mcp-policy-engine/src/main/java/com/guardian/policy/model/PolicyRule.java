package com.guardian.policy.model;

import java.util.List;

public record PolicyRule(
        String name,
        String description,
        Action action,
        List<String> methods,
        List<String> keywords,
        List<String> roles
) {
    public enum Action {
        DENY, ALLOW
    }
}
