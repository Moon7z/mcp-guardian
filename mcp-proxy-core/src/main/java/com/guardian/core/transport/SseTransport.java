package com.guardian.core.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.core.exception.McpProxyException;
import com.guardian.core.interceptor.InterceptorChain;
import com.guardian.core.interceptor.InterceptorContext;
import com.guardian.core.model.DownstreamServer;
import com.guardian.core.model.McpError;
import com.guardian.core.model.McpRequest;
import com.guardian.core.model.McpResponse;
import com.guardian.core.router.McpRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class SseTransport {

    private static final Logger log = LoggerFactory.getLogger(SseTransport.class);
    private final WebClient webClient;
    private final McpRouter router;
    private final InterceptorChain interceptorChain;
    private final ObjectMapper objectMapper;

    public SseTransport(WebClient mcpWebClient, McpRouter router,
                        InterceptorChain interceptorChain, ObjectMapper objectMapper) {
        this.webClient = mcpWebClient;
        this.router = router;
        this.interceptorChain = interceptorChain;
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> connectSse(String serverId, String sessionId, String userId) {
        DownstreamServer server = router.resolve(serverId)
                .orElseThrow(() -> new McpProxyException(
                        McpError.invalidRequest("Unknown server: " + serverId)));

        String sseUrl = server.url() + "/sse";
        log.info("Establishing SSE connection to downstream: {} ({})", serverId, sseUrl);

        return webClient.get()
                .uri(sseUrl)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> ServerSentEvent.<String>builder().data(data).build())
                .doOnSubscribe(s -> log.debug("SSE subscription started for server: {}", serverId))
                .doOnTerminate(() -> log.debug("SSE connection terminated for server: {}", serverId))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("SSE connection error for {}: {}", serverId, ex.getMessage());
                    String errorJson = toErrorJson(null,
                            McpError.serverUnreachable("Connection failed: " + ex.getStatusCode()));
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error").data(errorJson).build());
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected SSE error for {}: {}", serverId, ex.getMessage(), ex);
                    String errorJson = toErrorJson(null,
                            McpError.internalError("Internal proxy error"));
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error").data(errorJson).build());
                });
    }

    public Mono<McpResponse> forwardRequest(String serverId, McpRequest request,
                                             String sessionId, String userId) {
        DownstreamServer server = router.resolve(serverId)
                .orElseThrow(() -> new McpProxyException(
                        McpError.invalidRequest("Unknown server: " + serverId)));

        InterceptorContext context = new InterceptorContext(sessionId, serverId, userId);

        return Mono.fromCallable(() -> interceptorChain.executePreHandle(request, context))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(processedRequest -> {
                    String messageUrl = server.url() + "/message";
                    log.debug("Forwarding {} to {}", processedRequest.method(), messageUrl);

                    return webClient.post()
                            .uri(messageUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(processedRequest)
                            .retrieve()
                            .bodyToMono(McpResponse.class);
                })
                .map(response -> interceptorChain.executePostHandle(request, response, context))
                .onErrorResume(McpProxyException.class, ex -> {
                    log.warn("Proxy exception: {}", ex.getMessage());
                    return Mono.just(McpResponse.error(request.id(), ex.getMcpError()));
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Downstream error for {}: {}", serverId, ex.getMessage());
                    return Mono.just(McpResponse.error(request.id(),
                            McpError.serverUnreachable("Downstream error: " + ex.getStatusCode())));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error forwarding to {}: {}", serverId, ex.getMessage(), ex);
                    return Mono.just(McpResponse.error(request.id(),
                            McpError.internalError("Internal proxy error")));
                });
    }

    private String toErrorJson(Object id, McpError error) {
        try {
            return objectMapper.writeValueAsString(McpResponse.error(id, error));
        } catch (JsonProcessingException e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }
}
