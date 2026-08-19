# elma-gohan V0.1 Demo 技术与产品方案

## 1. Demo 目标

第一代 Demo 不追求解决完整的“刷分识别”问题。

V0.1 的唯一目标是跑通：


> **定位 → 获取附近餐厅 → 硬条件过滤 → 基础风险过滤 → 排序 → 主动推荐一家 → 用户反馈**

并验证一个核心假设：

> 相比“随机选一家附近餐厅”，经过基础风险过滤后的推荐是否更容易让用户接受、降低踩坑感。

---

# 2. 产品定位

产品面向：

**18～30 岁年轻用户。**

主要解决：

> “不知道吃什么，也不想花十几分钟研究，你直接给我一个不太容易踩坑的选择。”

产品不追求：

> 推荐全城最好吃的餐厅。

而追求：

> **Low Regret：降低用户吃完后悔的概率。**

---

# 3. 第一代产品原则

## 3.1 主动决策

默认只推荐：

**1 家。**

不是返回几十家餐厅让用户重新比较。

---

## 3.2 数据去水

不直接认为：

> 高评分 = 高质量。

平台评分只是推荐输入之一。

V0.1 先使用简单、透明的风险规则。

后续再逐步接入：

- 评论模板检测
- 评论时间异常
- 跨平台证据
- 近期口碑
- Embedding
- 异常检测模型

---

## 3.3 轻量化

V0.1 不引入：

- 微服务
- Python 服务
- Agent Runtime
- Kafka
- Elasticsearch
- 向量数据库
- Redis
- 推荐大模型
- RecBole
- PyOD

全部使用模块化单体完成。

---

## 3.4 开源

产品核心保持：

**免费、无广告、开源。**

核心推荐结果不得因为商业合作购买排名。

---

# 4. 第一代客户端形态

## 优先发布

**微信小程序**

技术：

```text
uni-app
Vue 3
TypeScript
```

原因：

- 无安装成本
- 年轻用户传播方便
- 微信生态定位、地图能力成熟
- 非常适合校园和朋友间测试
- 后续仍可基于 uni-app 输出 Android

---

## Android

V0.1 代码结构必须保持 Android 可适配。

但：

> **第一阶段不同时承担 Android 正式适配和小程序正式适配。**

顺序：

```text
微信小程序 Demo
↓
真实用户测试
↓
验证产品价值
↓
uni-app Android APK
↓
必要时未来迁移 Kotlin
```

后端完全保持不变。

---

# 5. Eat-Wheel 的使用原则

Eat-Wheel 只作为：

> **实现参考。**

不直接复制其代码作为项目基础。

主要参考：

- uni-app 小程序工程结构
- 定位授权流程
- `uni.getLocation`
- 距离选择
- 餐饮分类选择
- 结果卡片
- 微信地图导航
- Loading / Error 状态

不保留：

- 客户端直接请求地图 POI
- 客户端存放地图 Key
- 纯 `Math.random()` 推荐
- 客户端承担核心推荐
- 硬编码菜品推荐

本项目重新实现自己的客户端骨架。

---

# 6. what-to-eat-skill 的使用原则

仅作为：

> **推荐逻辑与高德 POI 使用方式参考。**

不引入其：

- Python Runtime
- Agent
- SKILL.md 执行体系

不直接复用其代码。

借鉴：

- 高德 Web Service
- 地理位置 + 周边 POI
- 距离
- 预算
- 品类
- 评分
- 推荐理由

所有实际逻辑重新使用 Java 实现。

---

# 7. 总体架构

```text
┌──────────────────────────────┐
│      uni-app / Vue3 / TS     │
│                              │
│ 定位                         │
│ 条件输入                     │
│ 推荐结果展示                 │
│ 导航                         │
│ 用户反馈                     │
└──────────────┬───────────────┘
               │ REST
               ▼
┌──────────────────────────────┐
│      Spring Boot / Java17    │
│                              │
│ RecommendationApplication    │
│                              │
│ ┌──────────────────────────┐ │
│ │ PoiProvider              │ │
│ └────────────┬─────────────┘ │
│              ▼               │
│       AmapPoiProvider        │
│                              │
│ ┌──────────────────────────┐ │
│ │ EvidenceProvider         │ │
│ └──────────────────────────┘ │
│ V0.1 默认 EmptyProvider      │
│                              │
│ ┌──────────────────────────┐ │
│ │ RiskEngine               │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ RecommendationEngine     │ │
│ └──────────────────────────┘ │
└──────────────┬───────────────┘
               ▼
         PostgreSQL
```

---

# 8. 架构原则

必须保持四个核心模块独立：

```java
public interface PoiProvider {
    List<Restaurant> nearby(
        Location location,
        SearchCondition condition
    );
}
```

```java
public interface EvidenceProvider {
    RestaurantEvidence getEvidence(Restaurant restaurant);
}
```

```java
public interface RiskEngine {
    RiskResult evaluate(
        Restaurant restaurant,
        RestaurantEvidence evidence
    );
}
```

```java
public interface RecommendationEngine {
    RecommendationResult recommend(
        List<RestaurantCandidate> candidates,
        UserPreference preference
    );
}
```

目的：

未来替换：

```text
高德
↓
腾讯
```

不用修改 RiskEngine。

未来：

```text
规则算法
↓
MinHash
↓
Embedding
↓
机器学习
```

不用修改客户端。

---

# 9. 客户端职责

客户端只负责：

```text
获取位置
↓
用户条件
↓
请求服务器
↓
展示推荐
↓
地图导航
↓
反馈
```

客户端禁止：

- 自己计算 RiskScore
- 自己计算推荐排名
- 自己请求多个评论平台
- 保存第三方服务敏感 Key
- 自己随机挑选最终餐厅

---

# 10. 首页 V0.1

核心页面保持极简。

```text
今天吃什么？

📍 当前定位

距离
[ 500m ][ 1km ][ 2km ][ 3km ]

预算
[ ¥20 ][ ¥40 ][ ¥70 ][ 不限 ]

类型
[ 随便 ▼ ]

不想吃：
[ 可选 ]

        帮我选
```

核心视觉中心：

> **帮我选**

---

# 11. 推荐结果

服务器返回一条主要推荐。

示例：

```text
今天吃这个

老街牛肉粉

620m
步行约 8 分钟
人均约 ¥26

风险：低

为什么推荐：
✓ 距离近
✓ 预算合适
✓ 基础评分稳定
✓ 数据完整度较高

[ 就它了 ]

换一家
```

---

# 12. “换一家”机制

不能无限刷新。

V0.1：

> 最多提供 3 个候选。

第一次：

A

第二次：

B

第三次：

C

再继续操作时：

> 三家里面仍然优先推荐 A。

避免产品再次制造选择困难。

---

# 13. POI 数据源

V0.1：

# 高德 Web Service

负责：

- 店铺名称
- POI ID
- 经纬度
- 距离
- 地址
- 分类
- 原始评分
- 人均价格
- 营业信息
- 标签

具体字段以实际 API 返回能力为准。

---

# 14. Restaurant 标准模型

第三方数据不能直接进入业务核心。

统一转为内部模型：

```text
Restaurant

id
source
sourcePoiId

name
latitude
longitude
distance

category

rating
reviewCount
averagePrice

businessStatus
openingHours

address

dataCompleteness
```

这样未来换 Provider 不影响 RecommendationEngine。

---

# 15. EvidenceProvider

V0.1 必须预留：

```java
EvidenceProvider
```

但默认实现：

```text
EmptyEvidenceProvider
```

即：

> 第一阶段允许没有小红书、B站、点评评论数据。

原因：

V0.1 先验证产品闭环。

以后增加：

```text
XiaohongshuEvidenceProvider

BilibiliEvidenceProvider

ReviewEvidenceProvider
```

业务接口保持不变。

---

# 16. RiskEngine V0.1

第一代明确采用：

# 规则模型

不使用机器学习。

输入：

```text
评分
评价数量
价格
距离
信息完整度
营业状态
```

未来有 Evidence 后再加入：

```text
模板评论比例
评价突增
近期趋势
跨平台一致性
```

---

# 17. RiskScore 基本范围

```text
0 ~ 100
```

越高：

> 推荐风险越大。

例如：

```text
0~20
低风险

21~40
中低风险

41~60
中风险

61+
原则上不主动推荐
```

---

# 18. RiskEngine V0.1 示例规则

示意规则：

```text
评分 ≥ 4.5
风险 +0

评分 4.2~4.5
风险 +5

评分 4.0~4.2
风险 +15

评分 < 4.0
风险 +30
```

数据量不足：

```text
评价数量明显过低
+10~15
```

信息不完整：

```text
营业信息缺失
+10

价格信息缺失
+5
```

价格异常：

```text
明显高于同距离同品类
+10
```

注意：

> 具体阈值必须配置化。

禁止直接散落在代码中。

---

# 19. RiskResult

必须返回：

```text
riskScore
riskLevel
reasons[]
algorithmVersion
```

例如：

```json
{
  "riskScore": 18,
  "riskLevel": "LOW",
  "reasons": [
    "评分稳定",
    "店铺信息完整",
    "价格位于正常范围"
  ],
  "algorithmVersion": "risk-v0.1"
}
```

产品必须做到：

> 可解释。

---

# 20. RecommendationEngine V0.1

同样使用简单规则。

不做机器学习。

主要指标：

```text
基础质量
距离
预算匹配
品类匹配
数据完整度
风险
```

形成：

```text
LowRegretScore
```

---

# 21. 推荐策略

不要：

```text
所有餐厅
↓
random
```

而是：

```text
所有附近餐厅
↓
硬过滤
↓
RiskScore
↓
删除高风险
↓
LowRegretScore 排序
↓
Top K
↓
有限随机
↓
最终一家
```

建议：

```text
Top K = 5
```

然后进行加权随机。

目的：

既避免：

> 每次都只推荐第一名。

也避免：

> 完全随机。

---

# 22. 用户条件

V0.1：

```text
location
radius
budget
category
dislikes
```

其中：

### 必填

```text
location
```

### 默认

```text
radius = 1km
budget = 不限
category = 随便
```

---

# 23. 用户反馈

吃完或者推荐后允许反馈：

```text
👍 不错

😐 一般

👎 踩坑
```

内部：

```text
LIKE
NORMAL
DISLIKE
```

第一版：

> 先记录，不急着复杂训练。

V0.2 再逐渐影响 Taste Profile。

---

# 24. 必须记录推荐日志

每一次推荐必须保存：

```text
userId
requestCondition
candidateCount

recommendedRestaurantId

riskScore
lowRegretScore

riskAlgorithmVersion
recommendationAlgorithmVersion

createdAt
```

原因：

后期可以比较：

```text
risk-v0.1
vs
risk-v0.2
```

到底谁的踩坑率更低。

---

# 25. 数据库 V0.1

核心表：

```text
restaurant
```

保存标准化店铺基础数据。

---

```text
risk_result
```

```text
restaurant_id
risk_score
risk_level
reasons_json
algorithm_version
calculated_at
```

---

```text
recommendation_log
```

保存每一次系统选择。

---

```text
user_feedback
```

保存：

```text
LIKE
NORMAL
DISLIKE
```

---

```text
user_preference
```

V0.1 只保存简单偏好。

---

# 26. API V0.1

## 获取推荐

```http
POST /api/v1/recommendations
```

请求：

```json
{
  "latitude": 28.0,
  "longitude": 112.0,
  "radius": 1000,
  "maxBudget": 40,
  "category": "ANY",
  "dislikes": []
}
```

响应：

```json
{
  "recommendationId": "...",
  "restaurant": {},
  "risk": {},
  "reasons": [],
  "alternativesRemaining": 2
}
```

---

## 换一家

```http
POST /api/v1/recommendations/{id}/reroll
```

不能重新从所有 POI 乱抽。

必须从该次请求的优质候选集中选择下一家。

---

## 用户反馈

```http
POST /api/v1/recommendations/{id}/feedback
```

```json
{
  "result": "LIKE"
}
```

---

# 27. 第一阶段不实现登录复杂体系

Demo 用户身份可以使用：

```text
匿名 UUID
```

微信登录：

> 可以后续补。

目的：

降低 MVP 开发成本。

但数据库模型保留：

```text
userId
```

---

# 28. 导航

客户端获得推荐结果后：

用户点击：

> 就它了

直接使用微信地图 / 系统地图打开餐厅位置。

导航不是核心业务逻辑。

---

# 29. Android 适配原则

第一代以微信小程序验收。

但所有客户端业务不得直接使用大量：

```text
wx.xxx
```

应优先封装：

```text
LocationService

NavigationService

PlatformService
```

内部再根据：

```text
MP-WEIXIN
APP-PLUS
```

做平台实现。

从第一天减少 Android 迁移成本。

---

# 30. V0.1 不做

明确禁止范围：

- 评论社区
- 商家入驻
- 团购
- 外卖
- 点餐
- 支付
- 广告
- 排行榜
- 社交
- 视频流
- 用户发帖
- 大模型聊天
- NLP 模型
- 全量评论爬虫
- 微服务
- Docker 集群
- 消息队列
- 推荐系统框架
- 向量搜索

不要因为“以后可能需要”提前实现。

---

# 31. V0.2 扩展接口

第一版架构必须允许后续增加：

```text
EvidenceProvider
        ↓
评论/社媒
        ↓
Template Detection
        ↓
MinHash
        ↓
Recent Trend
        ↓
Cross Platform Consistency
        ↓
RiskEngine v0.2
```

而无需重写：

```text
客户端
推荐流程
数据库主结构
POI 层
```

---

# 32. 后续数据去水路线

建议演进顺序：

## V0.1

```text
POI + 基础规则
```

## V0.2

```text
外部评论
+
MinHash 模板检测
+
时间异常
```

## V0.3

```text
多平台 Evidence
+
近期趋势
+
平台一致性
```

## V1.0

视数据量决定：

```text
Embedding
异常检测
个性化推荐
```

不是反过来。

---

# 33. 项目目录建议

## Backend

```text
backend/
├─ controller/
├─ application/
├─ domain/
│  ├─ restaurant/
│  ├─ risk/
│  ├─ recommendation/
│  └─ user/
├─ provider/
│  ├─ poi/
│  │  ├─ PoiProvider.java
│  │  └─ amap/
│  └─ evidence/
├─ infrastructure/
│  ├─ persistence/
│  └─ external/
└─ config/
```

采用：

> 模块化单体。

不要做形式主义 DDD。

主要目标是：

> Risk、Recommendation、Provider 边界清楚。

---

## Frontend

```text
frontend/
├─ src/
│  ├─ api/
│  │  └─ recommendation.ts
│  ├─ components/
│  ├─ pages/
│  │  ├─ home/
│  │  └─ result/
│  ├─ services/
│  │  ├─ location.ts
│  │  ├─ navigation.ts
│  │  └─ platform.ts
│  ├─ stores/
│  ├─ types/
│  └─ utils/
```

全面使用 TypeScript。

---

# 34. Demo 验收标准

V0.1 完成必须同时满足：

### 定位

- 用户允许定位后可以获取当前位置
- 拒绝权限有明确提示
- 不申请后台定位

### POI

- 可以获取指定距离内真实餐厅
- 结果具备基本店铺信息

### 过滤

能够按照：

- 距离
- 预算
- 品类
- 营业状态

过滤。

### RiskEngine

每个候选都有：

```text
RiskScore
RiskLevel
Reasons
AlgorithmVersion
```

### 推荐

系统返回：

> 1 家主推荐。

不是完整餐厅列表。

### 换一家

最多提供两次主要替换。

### 导航

可以跳转地图。

### 反馈

能够提交：

```text
👍 / 😐 / 👎
```

### 日志

每次推荐和反馈都可以追踪。

---

# 35. Demo 成功标准

技术成功：

> 完整闭环没有人工介入。

产品成功：

找至少：

> 10～30 名年轻目标用户

实际使用。

重点观察：

```text
推荐接受率

平均换店次数

LIKE / NORMAL / DISLIKE

再次使用情况
```

第一阶段最重要的指标：

# 踩坑反馈率

如果：

> RiskEngine v0.2

相比：

> risk-v0.1

能够持续降低 `DISLIKE`，

说明我们的核心差异化成立。

---

# 36. 第一代最终技术栈

正式确定：

### Frontend

```text
uni-app
Vue 3
TypeScript
```

### Backend

```text
Java 17
Spring Boot
```

### Database

```text
PostgreSQL
```

### POI

```text
高德 Web Service
```

### Risk

```text
Java 可配置规则
```

### Recommendation

```text
Java Ranking
+
Top-K 有限随机
```

### Evidence

```text
V0.1 只预留接口
```

### AI / Python

```text
V0.1 不引入
```

---

# 37. 最终架构结论

第一代 Demo 正式采用：

# 模块化单体 + Provider 抽象 + 可版本化 RiskEngine + Ranking Engine

Eat-Wheel：

> 只参考客户端实现方式，重新编写。

what-to-eat-skill：

> 只参考高德 POI 和推荐交互逻辑，不接入其代码和运行时。

第一代最重要的不是算法精度达到最终水平。

而是建立一个以后可以持续进化的闭环：

```text
真实餐厅
↓
RiskEngine
↓
安全候选池
↓
LowRegret Ranking
↓
推荐
↓
真实用户反馈
↓
下一代 RiskEngine
```

只要这个闭环搭稳，后续：

> 评论去水、MinHash、多平台数据、Embedding、异常检测、用户画像

都可以逐步添加，而不需要推倒重写。
