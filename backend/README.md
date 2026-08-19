# ELMA 家今天的饭 V0.12 后端

Java 17 + Spring Boot 3.5 + PostgreSQL + 高德 Web Service 的模块化单体。接口契约见 [`../contracts/openapi.yaml`](../contracts/openapi.yaml),实施计划见 [`../docs/backend-implementation-plan.md`](../docs/backend-implementation-plan.md)。

## 构建与测试

```bash
cd backend
mvn test              # 单元测试 + 集成测试(需要本机 PostgreSQL,见下)
mvn spring-boot:run   # 启动开发服务(默认 8080)
```

## 环境变量

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | PostgreSQL 地址与开发库名 | `localhost` / `5432` / `elma` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `postgres` / 空 |
| `AMAP_KEY` | 高德 Web Service Key,**只能放环境变量** | 空(未配置时推荐接口返回 502) |
| `AMAP_BASE_URL` | 高德 API 地址(测试 stub 用) | `https://restapi.amap.com` |
| `DB_TEST_NAME` | 测试库名(仅 `src/test/resources/application.yml`) | `elma_test` |

本地准备:创建 `elma` 与 `elma_test` 两个数据库,Flyway 启动时自动建表。`DB_PASSWORD` 等口令一律走环境变量,仓库不含任何真实凭据。

## 架构

```
controller/        三个 POST 接口,DTO 严格对齐 openapi.yaml
application/       RecommendationService:会话、候选池、reroll 游标、反馈编排
domain/
  restaurant/      Restaurant 标准模型(第三方数据必须先转此模型)
  risk/            RuleBasedRiskEngine:可配置规则,阈值全在 application.yml(elma.risk.*)
  recommendation/  品类硬过滤 -> 高风险剔除 -> LowRegretScore -> 多样化重排 -> 6 家候选池
provider/
  poi/             PoiProvider + AmapPoiProvider/AmapClient/AmapResponseMapper
  evidence/        EvidenceProvider + EmptyEvidenceProvider
infrastructure/    JPA 实体/仓库、全局异常处理、traceId 过滤器
config/            Amap/Risk/Recommendation 配置属性
```

## 人工验收

1. 设置 `AMAP_KEY` 与 `DB_PASSWORD`,启动 `mvn spring-boot:run`。
2. `curl -X POST localhost:8080/api/v1/recommendations -H "Content-Type: application/json" -H "X-Anonymous-User-Id: <uuid>" -d '{"latitude":28.2282,"longitude":112.9388}'` -> 201,返回一家真实餐厅。
3. 用返回的 `recommendationId` 依次调 `/api/v1/recommendations/<id>/reroll`（测试版最多 5 次，耗尽回初始推荐）与 `/api/v1/recommendations/<id>/feedback`（`{"result":"LIKE"}`）。
