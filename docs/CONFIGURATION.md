# MCP Guardian 配置指南

## 概述

MCP Guardian 使用 Spring Boot 的多环境配置机制，支持通过环境变量、命令行参数和配置文件进行灵活配置。遵循 [12-Factor App](https://12factor.net/) 原则，所有关键配置均可通过环境变量覆盖。

## 环境变量参考

### 服务器配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `SERVER_PORT` | 服务器端口 | `8080` |
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `dev` |

### Guardian 代理配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `GUARDIAN_CONNECT_TIMEOUT` | 连接超时时间（毫秒） | `5000` |
| `GUARDIAN_READ_TIMEOUT` | 读取超时时间（毫秒） | `30000` |

### 下游 MCP 服务器配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `GUARDIAN_DB_TOOLS_URL` | 数据库工具服务器 URL | `http://localhost:3001` |
| `GUARDIAN_DB_TOOLS_HEALTH_URL` | 数据库工具健康检查 URL | `http://localhost:3001/health` |
| `GUARDIAN_DB_TOOLS_ENABLED` | 是否启用数据库工具服务器 | `true` |
| `GUARDIAN_FILE_TOOLS_URL` | 文件工具服务器 URL | `http://localhost:3002` |
| `GUARDIAN_FILE_TOOLS_HEALTH_URL` | 文件工具健康检查 URL | `http://localhost:3002/health` |
| `GUARDIAN_FILE_TOOLS_ENABLED` | 是否启用文件工具服务器 | `true` |
| `GUARDIAN_CODE_TOOLS_URL` | 代码分析工具服务器 URL | `http://localhost:3003` |
| `GUARDIAN_CODE_TOOLS_HEALTH_URL` | 代码分析工具健康检查 URL | `http://localhost:3003/health` |
| `GUARDIAN_CODE_TOOLS_ENABLED` | 是否启用代码分析工具服务器 | `false` |

### JWT 认证配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `JWT_SECRET` | JWT 签名密钥（至少 32 字节） | 内置默认值（必须在生产环境修改） |
| `JWT_EXPIRATION` | Token 过期时间（毫秒） | `86400000`（24 小时） |

### 策略引擎配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `GUARDIAN_POLICY_DEFAULT_ACTION` | 默认策略动作 | `ALLOW` |

### 日志配置

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `LOG_LEVEL` | 全局日志级别 | `INFO` |
| `GUARDIAN_LOG_LEVEL` | Guardian 组件日志级别 | `DEBUG` |

## 配置文件

### 配置文件结构

```
mcp-gateway-app/src/main/resources/
├── application.yaml          # 基础配置（包含所有环境变量占位符）
├── application-dev.yaml      # 开发环境配置
├── application-test.yaml     # 测试环境配置
└── application-prod.yaml     # 生产环境配置
```

### 配置优先级（从高到低）

1. 命令行参数（`--key=value`）
2. 环境变量（`KEY=value`）
3. 环境配置文件（`application-{profile}.yaml`）
4. 基础配置文件（`application.yaml`）

### 环境说明

| 环境 | Profile | 端口 | 日志级别 | 超时时间 |
|-----|---------|-----|---------|---------|
| 开发 | `dev` | 8080 | DEBUG | 连接 5s / 读取 30s |
| 测试 | `test` | 8081 | INFO | 连接 3s / 读取 10s |
| 生产 | `prod` | 8080 | WARN | 连接 10s / 读取 60s |

## 部署示例

### 开发环境

```bash
# 使用默认配置（dev profile）
java -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar

# 显式指定 dev profile
java -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 测试环境

```bash
java -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=test
```

### 生产环境

```bash
# 使用环境变量
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=your-production-secret-key-at-least-32-bytes
export GUARDIAN_DB_TOOLS_URL=http://prod-db-tools:3001
export GUARDIAN_FILE_TOOLS_URL=http://prod-file-tools:3002

java -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar
```

```bash
# 或使用命令行参数
java -jar mcp-gateway-app-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --JWT_SECRET=your-production-secret-key-at-least-32-bytes \
  --GUARDIAN_DB_TOOLS_URL=http://prod-db-tools:3001
```

### Docker 部署

```bash
# 基本运行
docker run -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=your-production-secret-key-at-least-32-bytes \
  -e GUARDIAN_DB_TOOLS_URL=http://db-tools:3001 \
  -e GUARDIAN_FILE_TOOLS_URL=http://file-tools:3002 \
  -p 8080:8080 \
  mcp-guardian:latest
```

**docker-compose.yml 示例：**

```yaml
version: '3.8'
services:
  mcp-guardian:
    image: mcp-guardian:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${JWT_SECRET}
      - GUARDIAN_DB_TOOLS_URL=http://db-tools:3001
      - GUARDIAN_FILE_TOOLS_URL=http://file-tools:3002
      - GUARDIAN_CONNECT_TIMEOUT=10000
      - GUARDIAN_READ_TIMEOUT=60000
      - LOG_LEVEL=WARN
      - GUARDIAN_LOG_LEVEL=WARN
    depends_on:
      - db-tools
      - file-tools

  db-tools:
    image: mcp-db-tools:latest
    ports:
      - "3001:3001"

  file-tools:
    image: mcp-file-tools:latest
    ports:
      - "3002:3002"
```

### Kubernetes 部署

**ConfigMap：**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mcp-guardian-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  GUARDIAN_DB_TOOLS_URL: "http://db-tools-svc:3001"
  GUARDIAN_FILE_TOOLS_URL: "http://file-tools-svc:3002"
  GUARDIAN_CONNECT_TIMEOUT: "10000"
  GUARDIAN_READ_TIMEOUT: "60000"
  LOG_LEVEL: "WARN"
  GUARDIAN_LOG_LEVEL: "WARN"
```

**Secret：**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mcp-guardian-secret
type: Opaque
stringData:
  JWT_SECRET: "your-production-secret-key-at-least-32-bytes"
```

**Deployment：**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-guardian
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mcp-guardian
  template:
    metadata:
      labels:
        app: mcp-guardian
    spec:
      containers:
        - name: mcp-guardian
          image: mcp-guardian:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: mcp-guardian-config
            - secretRef:
                name: mcp-guardian-secret
          readinessProbe:
            httpGet:
              path: /mcp/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /mcp/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
```

## 配置验证

MCP Guardian 在启动时会自动验证配置：

- 检查下游服务器 URL 格式是否有效
- 检查超时时间是否在合理范围内
- 检查 JWT 密钥长度是否满足最低要求
- 检查是否使用了默认密钥（生产环境警告）
- 记录所有已配置的服务器及其状态

验证结果会输出到日志中，格式如下：

```
=== MCP Guardian Configuration Validation ===
Active profiles: [prod]
Proxy connect-timeout: 10000ms, read-timeout: 60000ms
Server 'db-tools' (Database Tools Server): http://prod-db-tools:3001
Server 'file-tools' (File System Tools Server): http://prod-file-tools:3002
Server 'code-tools': disabled
Total enabled downstream servers: 2
JWT token expiration: 86400000ms (24h), secret length: 48 bytes
=== Configuration validation completed ===
```

## 安全注意事项

1. **JWT 密钥**：生产环境必须修改默认密钥，建议使用至少 64 字节的随机字符串
2. **敏感配置**：使用 Kubernetes Secret 或 Docker Secret 管理敏感信息，避免将密钥写入配置文件或环境变量文件
3. **日志级别**：生产环境建议使用 `WARN` 级别，避免输出敏感信息到日志
4. **网络隔离**：下游 MCP 服务器应部署在内部网络，不对外暴露
