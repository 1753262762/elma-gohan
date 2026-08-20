# ELMA 家今天的饭 V0.3

LowRegret V0.3 是 Java 模块化单体后端与 uni-app 微信小程序前端。它保留 V0.2 的 File Evidence、TasteProfile、品类筛选和 5 次 reroll，并以高德为主 POI、百度为第二 Evidence 来源，引入实体匹配和跨平台评分一致性风险。产品仍只返回一家主推荐，接口事实源是 [`contracts/openapi.yaml`](contracts/openapi.yaml)，V0.3 数据流与规则见 [`docs/V0.3-amap-baidu-consistency.md`](docs/V0.3-amap-baidu-consistency.md)。

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

后端百度链路由 `BAIDU_ENABLED` 控制，服务端 AK 只允许通过 `BAIDU_MAP_AK` 注入。百度失败、无匹配或字段缺失都不会中断高德主推荐；File Evidence 继续作为评论型扩展点。开发环境变量与降级规则见 [`backend/README.md`](backend/README.md)。

网络请求统一通过 `src/api/client.ts`，页面不得直接调用 `uni.request`。定位和权限设置分别通过 `src/services/location.ts` 与 `src/services/platform.ts`，页面不得直接调用 `wx.*`。
