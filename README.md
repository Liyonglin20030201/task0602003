# PR Review Bot

GitHub Pull Request 自动代码审查机器人。接收 GitHub Webhook，通过 RabbitMQ 异步排队，调用 Claude API 分析代码变更，可选在 Docker 沙箱中编译运行 Java 代码，并通过 WebSocket 实时推送审查进度到 Vue 3 面板。

## 架构

```
GitHub Webhook → Spring Boot Controller → RabbitMQ → Review Engine → Claude API
                                                          ↓
                                                    Docker Sandbox
                                                          ↓
                                              Post Comments to GitHub PR
                                                          ↓
                                              WebSocket → Vue 3 Dashboard
```

## 模块

| 模块 | 职责 |
|------|------|
| `pr-review-core` | 共享模型、JPA 实体、RabbitMQ 配置、工具类 |
| `pr-review-webhook` | 主应用入口，接收 GitHub Webhook，发布消息 |
| `pr-review-engine` | 消费消息，调用 Claude API 审查代码，回写 GitHub |
| `pr-review-sandbox` | Docker 沙箱，安全编译运行 Java 代码片段 |
| `pr-review-dashboard` | WebSocket 服务端，REST API，提供审查历史 |
| `dashboard-frontend` | Vue 3 + Vite 前端，实时显示审查进度 |

## 技术栈

- Java 17 + Spring Boot 3.3
- RabbitMQ (AMQP)
- Claude API (Anthropic)
- Docker Java API
- WebSocket (STOMP + SockJS)
- Vue 3 + Vite + Pinia + TypeScript
- H2 (dev) / PostgreSQL (prod)

## 快速开始

### 1. 启动基础设施

```bash
docker-compose up -d
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并填入：
- `GITHUB_WEBHOOK_SECRET` - GitHub Webhook 密钥
- `GITHUB_TOKEN` - GitHub Personal Access Token
- `CLAUDE_API_KEY` - Anthropic API Key

### 3. 编译运行后端

```bash
mvn clean package -DskipTests
mvn spring-boot:run -pl pr-review-webhook
```

### 4. 启动前端

```bash
cd dashboard-frontend
npm install
npm run dev
```

### 5. 构建沙箱镜像

```bash
cd pr-review-sandbox/sandbox-images/java17
docker build -t pr-review-sandbox-java17 .
```

### 6. 配置 GitHub Webhook

在 GitHub 仓库 Settings → Webhooks 中添加：
- Payload URL: `http://your-server:8080/api/webhooks/github`
- Content type: `application/json`
- Secret: 与 `GITHUB_WEBHOOK_SECRET` 一致
- Events: 勾选 `Pull requests`

## 访问

- 后端 API: http://localhost:8080
- RabbitMQ 管理: http://localhost:15672 (guest/guest)
- 前端面板: http://localhost:5173
- H2 Console: http://localhost:8080/h2-console
