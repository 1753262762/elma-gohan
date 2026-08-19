# ELMA API 契约说明

V0.12 的机器可读接口事实源是 [`openapi.yaml`](./openapi.yaml)。本文只解释已确定的跨端规则，不定义第二套 DTO。

## 接口范围

| 用途 | 方法与路径 | 请求 DTO | 成功响应 DTO |
| --- | --- | --- | --- |
| 推荐一家 | `POST /api/v1/recommendations` | `CreateRecommendationRequest` | `201 RecommendationResponse` |
| 换一家 | `POST /api/v1/recommendations/{id}/reroll` | 无请求体 | `200 RecommendationResponse` |
| 用户反馈 | `POST /api/v1/recommendations/{id}/feedback` | `SubmitFeedbackRequest` | `201 FeedbackResponse` |

三个接口都要求 `X-Anonymous-User-Id` 请求头，值为客户端首次启动生成并持久化的 UUID。`id` 是推荐会话 ID，不是餐厅 ID。

## 已确定规则

1. 前端定位和服务端 POI/导航坐标统一使用 GCJ-02。
2. 创建推荐只返回当前一家，不返回候选列表。
3. 服务端保存最多 6 个候选；首次推荐之外允许最多 5 次 reroll。候选耗尽后的调用返回初始推荐，不产生第七家。
4. `alternativesRemaining` 是前端是否显示“换一家”的唯一判断字段。
5. 反馈体遵循产品方案，仅包含 `LIKE`、`NORMAL` 或 `DISLIKE`。服务端根据推荐会话当前展示项确定餐厅，并在响应中返回实际关联的 `restaurantId`。
6. 风险分数、风险等级、风险理由、推荐理由和算法版本全部由服务端产生，前端只展示。
7. 高德 Web Service Key、第三方 POI 原始结构、RiskEngine 和排序过程均不进入接口响应。

## 品类筛选

`category` 只接受 `MEAL`、`FAST_FOOD`、`DESSERT_DRINK`、`ANY`。缺省为 `MEAL`，用户无需先配置筛选条件；前端只提供可选纠偏。餐厅响应继续返回更细的品类 code 与 label，映射和多样化重排由服务端负责。

## DTO 摘要

### CreateRecommendationRequest

- 必填：`latitude`、`longitude`。
- 默认：`radius=1000`、`maxBudget=null`、`category=MEAL`、`dislikes=[]`。
- `radius` 只允许 500、1000、2000、3000 米。
- `maxBudget` 单位为人民币元，`null` 表示不限。

### RecommendationResponse

- `recommendationId`：后续 reroll 和 feedback 使用的推荐会话 ID。
- `restaurant`：导航和展示所需的标准餐厅摘要。
- `risk`：可解释、带版本的风险结果。
- `reasons`：服务端生成的推荐理由。
- `alternativesRemaining`：剩余未展示替代项数量，范围 0～5。

### SubmitFeedbackRequest / FeedbackResponse

- 请求只有 `result`。
- 响应返回反馈 ID、推荐会话 ID、实际餐厅 ID、反馈值和记录时间。

### ErrorResponse

所有业务错误统一返回 `code`、`message`、可选 `fieldErrors` 和 `traceId`。前端按稳定的 `code` 分支处理，`message` 可直接展示，`traceId` 用于排查。
