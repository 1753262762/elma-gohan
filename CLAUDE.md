# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 仓库现状

这是「ELMA 家今天的饭」前后端仓库。接口仍为 V0.2，当前排序与风险算法为 `recommendation-v0.3` / `risk-v0.3`：保留默认正餐、四类筛选、候选多样化和最多 5 次重新选择，并提供 File Evidence 与 TasteProfile 闭环。接口事实源是 [contracts/openapi.yaml](contracts/openapi.yaml)，当前算法增量规则见 [docs/recommendation-algorithm-notes.md](docs/recommendation-algorithm-notes.md)。

两份方案文档（`elma-gohan_V0.1_Demo_技术与产品方案.md`、`elma-gohan产品介绍.md`）是项目起点，不得覆盖或重写。

## 常用命令

验证 OpenAPI 契约（唯一已存在的可执行检查，修改 `contracts/openapi.yaml` 后必须运行）：

```bash
python contracts/validate_openapi.py
```

需要 `pyyaml`。脚本会校验操作集合（只允许三个 POST 接口）、operationId、`X-Anonymous-User-Id` 头、反馈 DTO 形状以及所有 example/default 与 schema 一致，输出 `CONTRACT_OK` 或 `CONTRACT_INVALID`。

后端工程在 `backend/`（Java 17 + Spring Boot 3.5 + Maven 单模块）：

```bash
cd backend && mvn test
```

集成测试连本机 PostgreSQL 测试库 `elma_test`（环境变量 `DB_TEST_NAME`/`DB_USERNAME`/`DB_PASSWORD` 可覆盖）；高德在测试中被本地 stub 替代，不需要真实 Key。启动开发服务需 `DB_PASSWORD` 与 `AMAP_KEY` 环境变量，详见 [backend/README.md](backend/README.md)。

后端工程建立后的构建/测试命令在工程落地时补充到本节。

## 接口契约（事实源）

接口事实源是 [contracts/openapi.yaml](contracts/openapi.yaml)，说明见 [contracts/README.md](contracts/README.md)。前后端实现不得自行定义第二套 DTO 字段含义；改契约必须先改 YAML 并通过验证脚本。

三个接口（均要求 `X-Anonymous-User-Id` 请求头，值为客户端生成的匿名 UUID）：

- `POST /api/v1/recommendations` — 创建推荐会话，只返回一家（201）
- `POST /api/v1/recommendations/{id}/reroll` — 换一家（200）
- `POST /api/v1/recommendations/{id}/feedback` — 用户反馈，请求体只有 `result`（LIKE/NORMAL/DISLIKE）（201）

路径中的 `id` 是推荐会话 ID，不是餐厅 ID。

### 不可破坏的规则

1. 坐标统一 GCJ-02（前端定位、服务端 POI、导航一致）。
2. 服务端最多保存 6 个不同候选；首次推荐之外允许最多 5 次 reroll，耗尽后返回初始推荐，不产生第七家。
3. `alternativesRemaining`（0～5）是前端是否显示“换一家”的唯一判断字段。
4. 风险分数/等级/理由、推荐理由、算法版本全部由服务端产生，前端只展示。
5. 高德 Web Service Key 只从后端环境变量读取；Key、POI 原始结构、RiskEngine 和排序过程不得进入接口响应。
6. `radius` 只允许 500/1000/2000/3000 米；`maxBudget` 单位为人民币元，`null` 表示不限。
7. 业务错误统一 `ErrorResponse`（`code`/`message`/可选 `fieldErrors`/`traceId`），前端按稳定 `code` 分支处理。
8. 请求品类只允许 `MEAL`、`FAST_FOOD`、`DESSERT_DRINK`、`ANY`，缺省为 `MEAL`；响应继续给细品类代码和 `label`。

## 架构（目标形态）

模块化单体（Java 17 + Spring Boot + PostgreSQL + 高德 Web Service），包结构建议见技术方案第 33 节（controller / application / domain / provider / infrastructure / config）。

四个核心接口必须保持独立、可替换：

- `PoiProvider`（V0.1 实现 `AmapPoiProvider`）— 附近餐厅查询；第三方数据必须先转成内部 `Restaurant` 标准模型（含 `sourcePoiId`、`dataCompleteness` 等），原始结构不得进入业务核心。
- `EvidenceProvider` — `FileEvidenceProvider` 默认启用，第三方 DTO 必须映射成统一 `RestaurantEvidence`；失败逐餐厅降级，`EmptyEvidenceProvider` 保留作 fallback。
- `RiskEngine` — `risk-v0.3` 可配置规则模型（非 ML），输出五项 factors、`riskScore`(0~100)、`riskLevel`、`confidence`、`reasons[]` 和版本；高风险项（61+）不主动推荐。
- `RecommendationEngine` — 硬过滤 → Evidence/Risk → 高风险剔除 → LowRegretScore（含可信度校正与 TasteProfile）→ Top-10 多样化 → 有限加权随机 → 最多 6 家候选池。

推荐流程：定位 → POI 获取 → 硬过滤 → Evidence → Risk → 高风险过滤 → 排序 → 主动推荐一家 → 反馈更新画像。每次推荐必须落 `recommendation_log`（请求条件快照、候选数、首次推荐餐厅、两种算法版本、分数），用于后续比较不同 RiskEngine 版本的踩坑率。数据库用 Flyway 建表，核心表：`restaurant`、`risk_result`、`recommendation_log`、`recommendation_candidate`、`user_feedback`、`user_preference`。

## V0.2 明确不做

不因“以后可能需要”提前实现：登录、真实平台爬虫、支付/团购/外卖、排行榜、社交、Redis、消息队列、微服务、Python/AI Runtime、Embedding、向量搜索、协同过滤。真实远程 Evidence 的批量、缓存和超时隔离留待后续。

## Git 约定

当前集成分支为 `main`；新开发分支继续使用清晰、窄范围的 Conventional Commit。
