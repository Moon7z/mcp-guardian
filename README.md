# MCP Guardian

**企业级 MCP 安全代理网关** — 为 Model Context Protocol 生态提供安全合规、权限管控与审计追踪能力。

---

## 项目背景

随着大语言模型（LLM）在企业中的深度应用，越来越多的团队通过 MCP (Model Context Protocol) 将内部数据库、文件系统、代码仓库等敏感资源暴露给 LLM 工具调用。然而，这种直连模式带来了严重的安全隐患：

- **数据泄露风险**：LLM 返回的结果中可能包含手机号、身份证号、API Key 等敏感信息
- **越权操作风险**：缺乏细粒度的权限控制，任何用户都可能执行 `DROP TABLE` 等破坏性操作
- **审计缺失**：无法追踪谁在什么时间调用了哪些工具、传入了什么参数

MCP Guardian 正是为解决这些痛点而生。它作为一个透明代理层，部署在 MCP 客户端与下游 MCP Server 之间，无需修改任何现有系统即可获得企业级安全能力。

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

---

## 核心功能

### 1. 透明 MCP 代理 (mcp-proxy-core)

- 完整实现 MCP 协议（基于 JSON-RPC 2.0）的请求转发
- 支持 SSE (Server-Sent Events) 长连接透传
- 多下游 MCP Server 路由分发，通过 `serverId` 动态路由
- 拦截器链（Chain of Responsibility）架构，支持灵活扩展

### 2. 敏感数据脱敏 (mcp-dlp-filter)

自动检测并遮蔽响应中的敏感信息，所有匹配内容替换为 `[REDACTED_BY_GUARDIAN]`：

| 敏感类型 | 示例 | 说明 |
|---------|------|------|
| 中国手机号 | `138****5678` | 1[3-9] 开头的 11 位号码 |
| 身份证号 | `110101****1234` | 18 位身份证号（含末位 X） |
| 电子邮箱 | `u***@example.com` | 标准邮箱格式 |
| API Key | `sk-****` | sk-/ak-/key-/token-/secret- 前缀的密钥 |
| AWS 密钥 | `AKIA****` | AWS Access Key ID |
| 配置密码 | `password=****` | 配置文件中的密码字段 |
| 数据库连接串 | `jdbc://***@host` | 含密码的 JDBC/MongoDB/Redis 连接串 |
| 信用卡号 | `4111-****-****-1111` | Visa/MasterCard/AmEx 卡号 |
| 内网 IP | `192.168.***` | 10.x/172.16-31.x/192.168.x 私有地址 |
| Bearer Token | `Bearer ****` | HTTP Authorization 头中的令牌 |

### 3. 动态策略引擎 (mcp-policy-engine)

基于 YAML 配置的访问控制引擎，支持：

- **方法级拦截**：按 JSON-RPC method 匹配（如 `tools/call`）
- **关键词黑名单**：检测请求参数中的危险关键词（如 `drop`、`truncate`、`delete from`）
- **角色豁免**：指定角色（如 `admin`、`dba`）可绕过特定规则
- **默认策略**：可配置为 ALLOW（白名单模式）或 DENY（黑名单模式）

策略违规时返回标准 MCP 错误响应（Error Code: `-32602`），客户端无需特殊处理。

### 4. 异步审计日志 (mcp-audit-manager)

每次 Tool 调用自动记录完整审计轨迹：

```json
{
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "sessionId": "session-001",
  "userId": "alice",
  "serverId": "db-tools",
  "method": "tools/call",
  "paramsSummary": "{\"name\":\"query_db\",\"arguments\":{\"sql\":\"SELECT...\"}}",
  "resultSummary": "{\"rows\":[...]}",
  "policyBlocked": false,
  "policyRule": null,
  "dlpRedacted": true,
  "durationMs": 42,
  "timestamp": "2026-03-31T03:48:28Z"
}
```

- 异步写入，不阻塞主请求链路
- 结构化 JSON 格式，可直接对接 ELK Stack / 阿里云日志服务 / Splunk
- 内存环形缓冲区保留最近 1000 条记录，支持实时查询

---

## 技术架构

### 技术栈

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 语言 | Java 17+（推荐 Java 21） | Java 21 可启用虚拟线程处理高并发 |
| 框架 | Spring Boot 3.4+ | 企业级应用框架 |
| 异步通信 | Spring WebFlux / WebClient | 非阻塞 SSE 转发 |
| 安全 | Spring Security | JWT 无状态认证 |
| 序列化 | Jackson | JSON-RPC 2.0 消息处理 |
| 构建 | Maven 多模块 | 关注点分离，独立部署 |

### 模块结构

```
mcp-guardian/
├── mcp-proxy-core/          # 核心代理：协议处理、路由、SSE 传输、拦截器链
│   ├── model/               #   JSON-RPC 2.0 DTO（McpRequest/McpResponse/McpError）
│   ├── transport/           #   SSE 传输层 & REST 控制器
│   ├── router/              #   下游服务路由
│   ├── interceptor/         #   拦截器链框架
│   └── exception/           #   统一异常处理
├── mcp-dlp-filter/          # DLP 脱敏模块
│   ├── pattern/             #   敏感数据正则模式库
│   └── filter/              #   脱敏拦截器 & 递归 JSON 遍历
├── mcp-policy-engine/       # 策略引擎模块
│   ├── engine/              #   规则评估器 & 策略拦截器
│   └── model/               #   策略规则模型
├── mcp-audit-manager/       # 审计日志模块
│   ├── service/             #   异步审计服务 & 审计拦截器
│   └── model/               #   审计记录模型
├── mcp-gateway-app/         # 网关启动模块
│   ├── SecurityConfig       #   Spring Security 配置
│   └── application.yaml     #   全局配置文件
└── docs/
    └── ARCHITECTURE.md      # 架构设计决策记录
```

### 请求处理流程

```
Client POST /mcp/{serverId}/message
  │
  ▼
McpProxyController                    ← 接收 JSON-RPC 请求
  │
  ▼
PolicyInterceptor [preHandle]         ← 策略检查：关键词 + 角色校验
  │ (违规则直接返回 -32602 错误)
  ▼
McpRouter.resolve(serverId)           ← 路由到下游 MCP Server
  │
  ▼
WebClient → Downstream Server         ← SSE/HTTP 转发
  │
  ▼
DlpInterceptor [postHandle]           ← 响应脱敏：递归扫描 JSON 树
  │
  ▼
AuditInterceptor [postHandle]         ← 异步记录审计日志
  │
  ▼
Client ← McpResponse                  ← 返回脱敏后的安全响应
```

---

## 快速开始

### 环境要求

- JDK 17+（推荐 JDK 21 以启用虚拟线程）
- Maven 3.8+

### 构建与运行

```bash
# 克隆项目
git clone https://github.com/Moon7z/mcp-guardian.git
cd mcp-guardian

# 编译 & 测试
mvn clean package

# 启动网关
java -jar mcp-gateway-app/target/mcp-gateway-app-1.0.0-SNAPSHOT.jar
```

网关默认监听 `http://localhost:8080`。

### 配置下游 MCP Server

编辑 `mcp-gateway-app/src/main/resources/application.yaml`：

```yaml
guardian:
  servers:
    db-tools:
      name: "Database Tools Server"
      url: "http://localhost:3001"
      health-check-url: "http://localhost:3001/health"
      enabled: true
    file-tools:
      name: "File System Tools Server"
      url: "http://localhost:3002"
      enabled: true
```

### 配置安全策略

```yaml
guardian:
  policy:
    default-action: ALLOW
    rules:
      - name: "block-destructive-sql"
        description: "禁止非管理员执行破坏性 SQL"
        action: DENY
        methods: ["tools/call"]
        keywords: ["drop", "truncate", "delete from"]
        roles: ["admin", "dba"]

      - name: "block-file-delete"
        description: "禁止非管理员删除文件"
        action: DENY
        methods: ["tools/call"]
        keywords: ["delete_file", "rm -rf", "remove_directory"]
        roles: ["admin"]
```

---

## API 接口

### SSE 连接

```bash
GET /mcp/{serverId}/sse

# Headers
X-Session-Id: session-001
X-User-Id: alice
```

建立与指定下游 MCP Server 的 SSE 事件流连接。

### 转发请求

```bash
POST /mcp/{serverId}/message
Content-Type: application/json

# Headers
X-Session-Id: session-001
X-User-Id: alice

# Body
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query_db",
    "arguments": {"sql": "SELECT * FROM users LIMIT 10"}
  }
}
```

### 健康检查

```bash
GET /mcp/health

# Response
{"status": "UP", "service": "mcp-guardian"}
```

---

## 客户端接入

将 MCP 客户端原本直连下游 Server 的地址改为指向 Guardian 网关即可，零侵入接入：

```json
{
  "mcpServers": {
    "db-tools": {
      "url": "http://localhost:8080/mcp/db-tools/sse"
    }
  }
}
```

---

## 许可证

MIT License
