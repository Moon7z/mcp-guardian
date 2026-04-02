# MCP Guardian 故障排查指南

## 常见问题

### 1. 启动失败

#### 问题：JWT 密钥长度不足
```
java.lang.IllegalArgumentException: JWT secret must be at least 32 bytes
```

**原因**：`JWT_SECRET` 环境变量或配置值少于 32 个字节。

**解决方案**：
```bash
# 生成安全的密钥
export JWT_SECRET=$(openssl rand -base64 48)
```

#### 问题：端口被占用
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案**：
```bash
# 修改端口
export SERVER_PORT=8081
# 或查找并终止占用端口的进程
lsof -i :8080   # Linux/Mac
netstat -ano | findstr :8080   # Windows
```

#### 问题：下游服务器 URL 无效
```
WARN  ConfigValidator - Server 'db-tools' has invalid URL: xxx
```

**原因**：`application.yaml` 或环境变量中配置的下游服务器 URL 格式错误。

**解决方案**：确保 URL 包含协议前缀（`http://` 或 `https://`），例如：
```bash
export GUARDIAN_DB_TOOLS_URL=http://localhost:3001
```

---

### 2. 认证问题

#### 问题：请求返回 401 Unauthorized
```json
{"jsonrpc":"2.0","error":{"code":-32600,"message":"Unauthorized: ..."}}
```

**排查步骤**：
1. 确认请求头包含 `Authorization: Bearer <token>`
2. 检查 token 是否过期：
   ```bash
   # 解码 JWT payload（base64）
   echo '<token-payload>' | base64 -d
   ```
3. 确认 token 使用了正确的密钥签名
4. 检查 token 中是否包含 `roles` 声明

#### 问题：请求返回 403 Forbidden

**原因**：用户角色不满足端点权限要求。`/mcp/**` 端点需要 `ROLE_USER` 或 `ROLE_ADMIN`。

**解决方案**：确认 JWT token 的 `roles` 声明中包含 `USER` 或 `ADMIN`。

---

### 3. 代理转发问题

#### 问题：请求超时
```
io.netty.channel.ConnectTimeoutException: connection timed out
```

**排查步骤**：
1. 确认下游 MCP 服务器正在运行
2. 检查网络连接：`curl http://localhost:3001/health`
3. 调整超时参数：
   ```bash
   export GUARDIAN_CONNECT_TIMEOUT=10000   # 连接超时（毫秒）
   export GUARDIAN_READ_TIMEOUT=60000      # 读取超时（毫秒）
   ```

#### 问题：Unknown server 错误
```json
{"error":{"code":-32600,"message":"Unknown server: xxx"}}
```

**原因**：请求路径中的 `serverId` 未在配置中注册或未启用。

**解决方案**：
1. 检查 `application.yaml` 中 `guardian.servers` 下是否配置了该服务器
2. 确认 `enabled` 为 `true`

---

### 4. DLP 脱敏问题

#### 问题：脱敏性能慢
```
WARN DlpRedactor - DLP JSON redaction took 120ms (threshold: 50ms)
```

**排查步骤**：
1. 检查响应数据大小 — 超大 JSON 会增加处理时间
2. 确认是否有异常的正则表达式回溯
3. 考虑调整日志级别以减少 DLP 日志输出：
   ```bash
   export GUARDIAN_LOG_LEVEL=WARN
   ```

#### 问题：误脱敏（正常数据被标记为敏感）

**排查步骤**：
1. 检查 `com.guardian.dlp.pattern.SensitivePatterns` 中的正则表达式
2. 查看日志中的 DLP 替换详情：
   ```
   DLP redacted 2 pattern(s): [EMAIL, PRIVATE_IP], replacements: {EMAIL=1, PRIVATE_IP=1}
   ```
3. 如果某个模式产生误报，可以修改正则或调整模式优先级

---

### 5. 策略引擎问题

#### 问题：请求被策略拦截
```json
{"error":{"code":-32600,"message":"Policy denied: block-destructive-sql"}}
```

**排查步骤**：
1. 检查 `application.yaml` 中 `guardian.policy.rules` 的规则配置
2. `roles` 字段定义了**豁免角色**（拥有这些角色的用户不受该规则限制）
3. 确认用户的 JWT token 中的角色是否在豁免列表中
4. 临时将 `default-action` 改为 `ALLOW` 以测试：
   ```bash
   export GUARDIAN_POLICY_DEFAULT_ACTION=ALLOW
   ```

---

### 6. 审计日志问题

#### 问题：审计日志中时间格式异常

**原因**：Jackson 缺少 `JavaTimeModule`，导致 `java.time.Instant` 序列化为数字而非 ISO 字符串。

**解决方案**：确保 `jackson-datatype-jsr310` 在类路径中，且 `application.yaml` 包含：
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
```

#### 问题：审计日志丢失

**排查步骤**：
1. 审计日志使用异步写入，确认审计线程池未饱和
2. 内存中最多保留 1000 条记录，旧记录会被淘汰
3. 检查 `AUDIT` logger 级别是否为 `INFO` 或更低

---

## 日志分析

### 日志格式

MCP Guardian 使用 JSON 格式日志输出：
```json
{"timestamp":"2024-01-01T12:00:00.000+08:00","level":"INFO","logger":"com.guardian...","thread":"main","message":"..."}
```

### 关键日志标识

| 日志内容 | 含义 |
|---------|------|
| `SSE connect request` | 新 SSE 连接请求 |
| `Forward request` | MCP 请求转发 |
| `DLP redacted N pattern(s)` | 数据脱敏触发 |
| `Policy denied` | 策略拒绝请求 |
| `JWT token expired` | Token 已过期 |
| `Invalid JWT signature` | Token 签名无效 |
| `Configuration validation` | 启动配置验证 |

### 调整日志级别

```bash
# 查看所有 Guardian 组件的详细日志
export GUARDIAN_LOG_LEVEL=DEBUG

# 仅查看警告和错误
export GUARDIAN_LOG_LEVEL=WARN

# 查看全局详细日志（包括 Spring 框架）
export LOG_LEVEL=DEBUG
```

---

## 性能调优

### 连接超时

根据网络环境调整超时设置：

| 环境 | 连接超时 | 读取超时 |
|-----|---------|---------|
| 本地开发 | 5000ms | 30000ms |
| 内网测试 | 3000ms | 10000ms |
| 跨地域生产 | 10000ms | 60000ms |

### JVM 参数

```bash
java -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar
```

### 监控端点

- **健康检查**：`GET /mcp/health` （无需认证）
- **API 文档**：`GET /swagger-ui.html` （无需认证）
- **OpenAPI 规范**：`GET /v3/api-docs` （无需认证）
