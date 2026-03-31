# MCP Guardian - Architecture Decision Record

## 1. System Overview

MCP Guardian is an enterprise-grade MCP (Model Context Protocol) proxy gateway that sits between
MCP clients and downstream MCP servers hosting enterprise data.

```
┌─────────────┐      ┌──────────────────────────────────────────────┐      ┌─────────────────┐
│  MCP Client  │─────>│              MCP Guardian                    │─────>│ MCP Server A    │
│              │<─────│                                              │<─────│ (DB Tools)      │
└─────────────┘  SSE │  ┌──────────┐ ┌─────────┐ ┌──────────────┐  │  SSE ├─────────────────┤
                      │  │  Policy   │ │   DLP   │ │    Audit     │  │      │ MCP Server B    │
                      │  │  Engine   │ │  Filter │ │   Manager    │  │      │ (File Tools)    │
                      │  └──────────┘ └─────────┘ └──────────────┘  │      └─────────────────┘
                      └──────────────────────────────────────────────┘
```

## 2. Key Architecture Decisions

### ADR-001: Transport Protocol - SSE over HTTP
- **Decision**: Use Server-Sent Events (SSE) as the primary transport between client and gateway,
  and between gateway and downstream MCP servers.
- **Rationale**: SSE is the standard MCP transport for remote servers. It provides unidirectional
  streaming from server to client over HTTP, with client-to-server communication via POST requests.
- **Consequence**: The gateway must maintain SSE connections to both upstream clients and downstream servers.

### ADR-002: Interceptor Chain Pattern
- **Decision**: Use a chain-of-responsibility pattern for request/response processing.
- **Rationale**: Allows pluggable, ordered processing stages (policy check -> forwarding -> DLP -> audit).
- **Chain Order**:
  1. **Inbound**: PolicyInterceptor -> Router -> Downstream Forwarding
  2. **Outbound**: DLP Filter -> Audit Logger -> Client Response

### ADR-003: Virtual Threads for Concurrency
- **Decision**: Use Java 21 Virtual Threads (Project Loom) for all blocking I/O operations.
- **Rationale**: Each proxied MCP session involves long-lived SSE connections. Virtual threads
  allow handling thousands of concurrent sessions without thread pool exhaustion.
- **Configuration**: Spring Boot 3.4+ `spring.threads.virtual.enabled=true`

### ADR-004: Multi-Module Maven Structure
- **Decision**: Separate concerns into distinct Maven modules.
- **Modules**:
  - `mcp-proxy-core` - Protocol handling, routing, transport
  - `mcp-dlp-filter` - Data Loss Prevention / redaction
  - `mcp-policy-engine` - Access control and policy enforcement
  - `mcp-audit-manager` - Async audit logging
  - `mcp-gateway-app` - Spring Boot application assembly

### ADR-005: Error Handling Strategy
- **Decision**: All downstream errors are caught and translated to standard MCP JSON-RPC error responses.
- **Error Codes**:
  - `-32600` Invalid Request
  - `-32601` Method Not Found
  - `-32602` Invalid Params (also used for policy violations)
  - `-32603` Internal Error
  - `-32000` Downstream Server Unreachable

## 3. Data Flow

### Request Flow
```
Client POST /mcp/{serverId}/message
  -> McpRequestController
    -> PolicyInterceptor.preHandle()     [allow/deny based on policies.yaml]
    -> McpRouter.route(serverId)         [resolve downstream server]
    -> SseForwarder.forward(request)     [send to downstream MCP server]
    -> DlpFilter.filter(response)        [redact sensitive data]
    -> AuditService.log(auditRecord)     [async audit logging]
  <- SSE event stream to client
```

### SSE Session Flow
```
Client GET /mcp/{serverId}/sse
  -> McpSseController
    -> Establish upstream SSE connection to client
    -> Establish downstream SSE connection to MCP server
    -> Bidirectional event relay with interceptor chain
```

## 4. Security Model

- **Authentication**: JWT-based identity verification via Spring Security
- **Authorization**: Policy engine evaluates tool-call permissions per user/role
- **Data Protection**: DLP filter redacts PII/secrets from all responses
- **Audit Trail**: Every tool invocation is logged with full context

## 5. Configuration Model

All runtime configuration is externalized in `application.yaml`:
- Downstream server registry with health check URLs
- DLP pattern definitions (regex-based)
- Policy rules (YAML-based blacklist/whitelist)
- Audit log destinations
