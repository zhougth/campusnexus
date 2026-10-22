# CampusNexus 前端

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus。

## 启动

```bash
npm install
npm run dev
```

默认地址 `http://localhost:5173`，`/api` 由 Vite 代理到 `http://localhost:8080`（见 `vite.config.ts`）。

## 构建

```bash
npm run build
npm run preview
```

## 前后端约定

- 统一响应 `code === 200 || code === 202` 视为成功；202 为异步任务创建（向量化、批次回滚），会弹通知并引导到任务页。
- JWT 存放在 `sessionStorage`：当前标签页刷新保持登录，关闭标签页失效；启动时通过 `GET /api/auth/me` 校验。
- 后端未实现接口（HTTP 404 + `code=404` + `message=资源不存在`）会在页面显示「后端接口尚未实现」，附带方法与路径；后端补齐后无需修改前端。
- 后端未启动时显示「无法连接后端服务」，与「接口未实现」分开提示。
- 聊天使用 `fetch` + `ReadableStream` 解析 SSE（`src/utils/sse.ts`），支持停止生成；未使用原生 EventSource。
- Markdown 渲染经 `markdown-it` → `highlight.js` → `DOMPurify` 净化。

## 页面

- 公共端：登录/注册、智能问答（三栏）、知识库列表/详情、提交贡献、我的贡献、个人中心
- 管理端：仪表盘、知识管理、文档上传、贡献审核、反馈处理、用户管理、分类管理、提问日志、导入批次、异步任务
