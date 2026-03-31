package com.guardian.core.interceptor;

import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class InterceptorChain {

    private static final Logger log = LoggerFactory.getLogger(InterceptorChain.class);
    private final List<McpInterceptor> interceptors;

    public InterceptorChain(List<McpInterceptor> interceptors) {
        this.interceptors = interceptors.stream()
                .sorted(Comparator.comparingInt(McpInterceptor::getOrder))
                .toList();
        log.info("Initialized interceptor chain with {} interceptors: {}",
                this.interceptors.size(),
                this.interceptors.stream().map(i -> i.getClass().getSimpleName()).toList());
    }

    public McpRequest executePreHandle(McpRequest request, InterceptorContext context) {
        McpRequest current = request;
        for (McpInterceptor interceptor : interceptors) {
            log.debug("Pre-handle: {}", interceptor.getClass().getSimpleName());
            current = interceptor.preHandle(current, context);
        }
        return current;
    }

    public McpResponse executePostHandle(McpRequest request, McpResponse response, InterceptorContext context) {
        McpResponse current = response;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            McpInterceptor interceptor = interceptors.get(i);
            log.debug("Post-handle: {}", interceptor.getClass().getSimpleName());
            current = interceptor.postHandle(request, current, context);
        }
        return current;
    }
}
