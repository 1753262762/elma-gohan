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

当前处于视觉确认门禁阶段：首页和结果页只用于确认“清透 ACG”设计方向。开发态结果页视觉预览地址为 `/pages/result/index?preview=1`；该临时样例会在真实 API 接入时删除。

## 配置与边界

`VITE_API_BASE_URL` 应包含 `/api/v1`。真实环境配置写入 `.env.local`，不要提交高德 Key 或其他敏感信息。前端不得实现风险计算、推荐排序、餐厅随机选择或高德 Web Service 调用。
