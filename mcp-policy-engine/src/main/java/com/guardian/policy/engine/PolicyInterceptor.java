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
        return 100;
    }

    @Override
    public McpRequest preHandle(McpRequest request, InterceptorContext context) {
        String userRole = context.getUserRole();

        PolicyEvaluator.EvaluationResult result = evaluator.evaluate(
                request.method(), request.params(), userRole);

        if (!result.allowed()) {
            log.warn("Policy blocked request: method={}, rule={}, reason={}, user={}, role={}",
                    request.method(), result.ruleName(), result.reason(),
                    context.getUserId(), userRole);
            context.setAttribute("policy.blocked", true);
            context.setAttribute("policy.rule", result.ruleName());
            throw McpProxyException.policyViolation(result.reason());
        }

        context.setAttribute("policy.blocked", false);
        return request;
    }
}
