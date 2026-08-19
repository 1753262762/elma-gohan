# ELMA 家今天的饭 V0.2

LowRegret V0.2 是 Java 模块化单体后端与 uni-app 微信小程序前端。V0.2 在已验收的 V0.12 筛选与 5 次 reroll 上加入外部 Evidence 去水、`risk-v0.2` 和用户反馈画像闭环。产品仍只返回一家主推荐，接口事实源是 [`contracts/openapi.yaml`](contracts/openapi.yaml)，数据流与规则见 [`docs/V0.2-evidence-risk-and-taste.md`](docs/V0.2-evidence-risk-and-taste.md)。

## 环境

- Node.js 22 LTS
- pnpm 11
- 微信开发者工具（真机验收时需要 AppID）

```powershell
pnpm install
Copy-Item .env.example .env.local
pnpm dev:mp-weixin
```

在微信开发者工具中导入 `dist/dev/mp-weixin`。本地 H5 视觉检查可运行 `pnpm dev:h5`。

## 验证

```powershell
pnpm typecheck
pnpm test:run
pnpm build:mp-weixin
```

当前闭环：首页默认正餐，可选纠偏为正餐、小吃快餐、饮品甜品或随便；服务端返回一家推荐并允许测试用户最多重新选择 5 次；“就它了”通过 `uni.openLocation` 打开当前餐厅；三种反馈会更新 TasteProfile，并从下一次新推荐开始影响排序。当前会话候选池冻结，reroll 不重复且不会被反馈重排。

## 配置与边界

`VITE_API_BASE_URL` 应包含 `/api/v1`。真实环境配置写入 `.env.local`，不要提交高德 Key 或其他敏感信息。前端不得实现风险计算、推荐排序、餐厅随机选择或高德 Web Service 调用。

后端 File Evidence 默认读取 `${EVIDENCE_FILE:classpath:evidence/restaurant-evidence.json}`；主资源文件为空，缺失、损坏或无匹配记录都会降级，不中断推荐。开发环境变量与文件格式见 [`backend/README.md`](backend/README.md)。

网络请求统一通过 `src/api/client.ts`，页面不得直接调用 `uni.request`。定位和权限设置分别通过 `src/services/location.ts` 与 `src/services/platform.ts`，页面不得直接调用 `wx.*`。
