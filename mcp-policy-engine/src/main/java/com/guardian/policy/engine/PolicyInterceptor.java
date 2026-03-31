package com.guardian.policy.engine;

import com.guardian.core.exception.McpProxyException;
import com.guardian.core.interceptor.InterceptorContext;
import com.guardian.core.interceptor.McpInterceptor;
import com.guardian.core.model.McpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicyInterceptor implements McpInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PolicyInterceptor.class);
    private final PolicyEvaluator evaluator;

    public PolicyInterceptor(PolicyEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public int getOrder() {
        return 100; // runs first, before forwarding
    }

    @Override
    public McpRequest preHandle(McpRequest request, InterceptorContext context) {
        String userRole = context.getUserId(); // simplified; in production, resolve from JWT claims

        PolicyEvaluator.EvaluationResult result = evaluator.evaluate(
                request.method(), request.params(), userRole);

        if (!result.allowed()) {
            log.warn("Policy blocked request: method={}, rule={}, reason={}",
                    request.method(), result.ruleName(), result.reason());
            context.setAttribute("policy.blocked", true);
            context.setAttribute("policy.rule", result.ruleName());
            throw McpProxyException.policyViolation(result.reason());
        }

        context.setAttribute("policy.blocked", false);
        return request;
    }
}
