# ELMA 家今天的饭前端

ELMA V0.1 的 uni-app 前端，优先交付微信小程序。产品只返回一家主推荐，接口实现必须以 [`contracts/openapi.yaml`](contracts/openapi.yaml) 为准。

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

当前已接通 V0.1 前端业务闭环：首页提交真实条件并展示一家服务端推荐；“换一家”只调用推荐会话的 reroll 接口并遵循服务端剩余次数；“就它了”通过 `uni.openLocation` 打开当前餐厅；三种反馈均提交到当前推荐会话，并按餐厅阻止重复提交。

## 配置与边界

`VITE_API_BASE_URL` 应包含 `/api/v1`。真实环境配置写入 `.env.local`，不要提交高德 Key 或其他敏感信息。前端不得实现风险计算、推荐排序、餐厅随机选择或高德 Web Service 调用。

网络请求统一通过 `src/api/client.ts`，页面不得直接调用 `uni.request`。定位和权限设置分别通过 `src/services/location.ts` 与 `src/services/platform.ts`，页面不得直接调用 `wx.*`。
