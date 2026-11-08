# CampusTrade 校园二手交易平台

面向广州工商学院的校园二手交易平台。学生在校内发布、检索、购买闲置物品，支持站内聊天、模拟支付、退款、买卖双方互评与信用分、系统通知，以及管理员后台审核与治理。

## 功能特性

| 模块 | 能力 |
|------|------|
| 用户 | 注册（学号 + 学校校验）、登录、个人资料、改密、头像上传 |
| 商品 | 发布、编辑、删除、下架、审核、分页检索、分类筛选、浏览计数 |
| 订单 | 下单（防超卖 + 幂等键）、支付（钱包冻结）、卖家发货、买家确认收货（结算给卖家）、取消、退款（申请/同意/拒绝）、30 分钟未支付由 RabbitMQ 延迟消息自动取消（定时任务兜底） |
| 钱包 | 可用 / 冻结双余额、充值、资金流水、余额与流水对账（管理端） |
| 评价 | 收货后买卖双方互评，平均分 / 评价数 / 好评率聚合为信用分 |
| 通知 | 订单、退款、评价事件站内通知，未读数轮询 |
| 聊天 | WebSocket/STOMP 实时聊天、图片消息、会话列表、未读数 |
| 收藏 / 评论 / 举报 / 公告 | 常规社区能力 |
| 管理后台 | 数据看板、用户管理、商品审核、举报处理、公告发布 |
| 基础设施 | JWT 无状态认证、统一异常与响应、Redis 缓存与限流（不可用时自动降级本地）、RabbitMQ 延迟队列、traceId + 慢 SQL 日志 + actuator、Knife4j 文档 |

## 技术栈

- 后端：Spring Boot 3.2、Spring Security 6、MyBatis-Plus 3.5、MySQL 8、Redis 7、RabbitMQ 3.13、WebSocket/STOMP、Knife4j
- 前端：Vue 3、Vite 5、Element Plus、Pinia、Vue Router、Axios
- 部署：Docker / Docker Compose / Nginx；CI：GitHub Actions（单测）

## 目录结构

```
CampusTrade/
├── backend/          # Spring Boot 后端
│   ├── db/           # init.sql、migration_v2.sql、migration_v3.sql
│   ├── src/          # 主代码（com.campustrade）与测试
│   ├── Dockerfile
│   └── pom.xml
├── frontend/         # Vue 3 前端
│   ├── src/          # 页面、组件、api、store、utils
│   ├── nginx.conf    # 前端容器内 Nginx 配置
│   ├── Dockerfile
│   └── package.json
├── docs/             # 需求、设计、数据库、API、测试、部署、手册、验收文档
│   └── 开题报告/      # 开题报告 docx / pdf
├── scripts/          # 验收、超时验证、数据脚本
├── nginx.conf        # 根 Nginx 配置（与 frontend/nginx.conf 内容一致）
└── docker-compose.yml
```

## 快速开始

### 1. 准备数据库

```bash
mysql -u root -p < backend/db/init.sql
# 已有库则执行增量迁移
mysql -u root -p campus_trade < backend/db/migration_v4_wallet.sql
# 可选：演示数据（4 个账号、8 个商品、3 笔订单、钱包与流水）
mysql -u root -p campus_trade < backend/db/seed.sql
```

默认连接：`localhost:3306/campus_trade`，账号 `root/123456`（见 `backend/src/main/resources/application.yml`）。

### 2. 启动 Redis 与 RabbitMQ（均可选）

Redis 用于商品详情、分类、未读消息缓存与限流，RabbitMQ 用于订单超时延迟消息。两者都不可用时：Redis 自动降级为进程内缓存/本地限流计数，RabbitMQ 投递失败只记日志，超时取消由 `OrderTimeoutTask` 兜底（`campustrade.order.mq-enabled=false` 可显式关闭 MQ）。

```bash
docker compose up -d redis rabbitmq
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或打包运行
mvn -DskipTests package
java -jar target/campus-trade-1.0.0.jar
```

- 接口地址：<http://localhost:8080/api>
- 在线接口文档：<http://localhost:8080/doc.html>
- 健康检查：<http://localhost:8080/actuator/health>
- WebSocket 端点：`/ws`（SockJS）

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5173>。生产构建：`npm run build`。

### 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| lwx | 123456 | 普通用户 |
| alice / bob / carol | 123456 | 演示用户（`seed.sql` 导入后可用） |

## 测试

```bash
cd backend
mvn test                                   # 单元测试（150 个用例，同时生成 JaCoCo 报告）
mvn test -Dtest=OrderConcurrencyIT -Dit=true   # 并发下单集成测试（需 MySQL）
powershell -ExecutionPolicy Bypass -File scripts/verify-business-v5.ps1  # 接口验收（19 项）
```

压测（不依赖 JMeter）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bench.ps1 `
  -Url "http://localhost:8080/api/goods/list?page=1&pageSize=12" -Concurrency 20 -DurationSeconds 15
```

端到端冒烟（需后端已启动、库已导入 `init.sql` + `seed.sql`）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/e2e-smoke.ps1     # 19 项：登录 / 下单 / 幂等 / 担保交易 / 对账 / 上传魔数 / 限流
powershell -ExecutionPolicy Bypass -File scripts/e2e-timeout.ps1   # 需以 timeout-minutes=1 启动，验证 MQ 到点取消
```

实测基线（单实例、本机回环、8 条商品）：`GET /api/goods/list` 20 并发 330 QPS、P99 91ms；`GET /api/order/my-buy` 10 并发 425 QPS、P99 42ms，错误率均为 0。数字只作前后对比基线，不代表生产容量。

覆盖率报告：`backend/target/site/jacoco/index.html`（当前 Service 层 85.2%、整体 54.3%）。

## 关键设计

- **防超卖**：下单执行 `UPDATE goods SET status = 3 WHERE id = ? AND status = 1`，影响行数为 0 即被抢先，直接拒绝。
- **状态机**：订单流转统一用 `UPDATE orders SET status = ? WHERE id = ? AND status = ?` 条件更新，重复点击/并发请求不会重复流转。
- **超时释放**：下单后发一条 TTL 延迟消息（`order.delay.queue`），过期经死信交换机转入 `order.timeout.queue` 由消费者取消订单并恢复商品在售；`OrderTimeoutTask` 每 5 分钟扫表仅作兜底（`idx_status_create_time` 支撑）。
- **担保交易**：`wallet` 可用/冻结双余额 + `trade_record` 流水；支付冻结、确认收货结算给卖家、退款解冻，`(order_id, type, user_id)` 唯一索引保证回调幂等，管理端可对账。
- **退款**：申请中订单禁止发货与确认收货；卖家同意后退款状态置 2、订单取消、商品恢复在售、资金解冻。
- **缓存降级**：`CacheService` 启动时探测 Redis，不可用自动切换本地缓存，读缓存失败时回源数据库。
- **幂等与限流**：`@Idempotent` 对下单做「锁 + 结果回放」，`@RateLimit` 用 Redis 固定窗口限制注册/登录/下单/充值；Redis 不可用时降级为本地计数。
- **可观测性**：`TraceIdFilter` 注入 `X-Trace-Id` 到 MDC，`SlowSqlInterceptor` 记录慢 SQL，日志按天滚动，暴露 `/actuator/health`。

