package com.guardian.core.interceptor;

import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;

public interface McpInterceptor {

    int getOrder();

    default McpRequest preHandle(McpRequest request, InterceptorContext context) {
        return request;
    }

    default McpResponse postHandle(McpRequest request, McpResponse response, InterceptorContext context) {
        return response;
    }
}
