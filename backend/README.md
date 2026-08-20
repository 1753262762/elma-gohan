# ELMA 家今天的饭 V0.3 后端

Java 17 + Spring Boot 3.5 + PostgreSQL 的模块化单体。高德提供主 POI，百度 Place 提供第二平台 Evidence；接口契约见 [`../contracts/openapi.yaml`](../contracts/openapi.yaml)，V0.3 数据流见 [`../docs/V0.3-amap-baidu-consistency.md`](../docs/V0.3-amap-baidu-consistency.md)。

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
| `BAIDU_MAP_AK` | 百度 Place 服务端 AK，**只能放环境变量** | 空（自动降级） |
| `BAIDU_ENABLED` | 是否启用百度第二 Evidence 来源 | `true` |
| `BAIDU_BASE_URL` | 百度 API 地址（测试 stub 用） | `https://api.map.baidu.com` |
| `EVIDENCE_PROVIDER` | Evidence 实现：`file` 或 `empty` | `file` |
| `EVIDENCE_FILE` | 标准化评论证据 JSON，可用外部文件覆盖 | `classpath:evidence/restaurant-evidence.json` |
| `DB_TEST_NAME` | 测试库名(仅 `src/test/resources/application.yml`) | `elma_test` |

本地准备:创建 `elma` 与 `elma_test` 两个数据库,Flyway 启动时自动建表。`DB_PASSWORD` 等口令一律走环境变量,仓库不含任何真实凭据。

## 架构

```
controller/        三个 POST 接口,DTO 严格对齐 openapi.yaml
application/       RecommendationService:会话、候选池、reroll 游标、反馈编排
domain/
  restaurant/      Restaurant 标准模型(第三方数据必须先转此模型)
  risk/            risk-v0.3:评分/模板/burst/趋势/数据不足/跨平台冲突
  recommendation/  高风险剔除 -> LowRegretScore + TasteProfile -> Top-10 -> 6 家候选池
provider/
  poi/             PoiProvider + AmapPoiProvider/AmapClient/AmapResponseMapper
  evidence/        File 评论 Evidence + 百度批量 Evidence + 实体匹配
infrastructure/    JPA 实体/仓库、Evidence 缓存、全局异常处理、traceId 过滤器
config/            Amap/Baidu/EntityResolution/Risk/Recommendation 配置属性
```

## File Evidence 格式

文件顶层为 `restaurants` 数组，每家以 POI 来源与外部 ID 对应餐厅，评论会映射成统一 `RestaurantEvidence`：

```json
{
  "restaurants": [{
    "source": "AMAP",
    "sourcePoiId": "B001",
    "fetchedAt": "2026-08-19T00:00:00Z",
    "reviews": [{
      "externalReviewId": "review-1",
      "text": "现场用餐记录",
      "rating": 4.5,
      "createdAt": "2026-08-18T12:00:00Z"
    }]
  }]
}
```

单餐厅最多读取 200 条。文件读取失败、JSON 损坏、无匹配餐厅或 Provider 异常都会转为 `UNAVAILABLE/NO_DATA`，主推荐继续运行。

## 人工验收

1. 设置 `AMAP_KEY`、`BAIDU_MAP_AK` 与 `DB_PASSWORD`，启动 `mvn spring-boot:run`。
2. `curl -X POST localhost:8080/api/v1/recommendations -H "Content-Type: application/json" -H "X-Anonymous-User-Id: <uuid>" -d '{"latitude":28.2282,"longitude":112.9388}'` -> 201,返回一家真实餐厅。
3. 检查响应中的 `evidenceSummary`；百度不可用时应为 `UNAVAILABLE`，推荐仍成功。
4. 用返回的 `recommendationId` 依次调 `/api/v1/recommendations/<id>/reroll`（测试版最多 5 次，耗尽回初始推荐）与 `/api/v1/recommendations/<id>/feedback`（`{"result":"LIKE"}`）。
