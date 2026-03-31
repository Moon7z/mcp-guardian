package com.guardian.policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "guardian.policy")
public class PolicyProperties {

    private String defaultAction;
    private List<RuleProperties> rules;

    public String getDefaultAction() { return defaultAction; }
    public void setDefaultAction(String defaultAction) { this.defaultAction = defaultAction; }
    public List<RuleProperties> getRules() { return rules; }
    public void setRules(List<RuleProperties> rules) { this.rules = rules; }

    public static class RuleProperties {
        private String name;
        private String description;
        private String action;
        private List<String> methods;
        private List<String> keywords;
        private List<String> roles;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}
