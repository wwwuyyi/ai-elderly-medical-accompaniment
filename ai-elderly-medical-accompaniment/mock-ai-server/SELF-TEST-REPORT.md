# 系统联调自测报告（2026-10-29）

## 环境状态
- 后端：SpringBoot 3.2.5, H2 dev profile, port 8080
- 前端：Vite 5.x, port 5173 proxy→8080
- Mock AI OCR: Node.js, port 9001
- Mock AI LLM: Node.js, port 9002

## 21项自测结果

### 家属端

| # | 项目 | 方法 | 证据 |
|---|---|---|---|
| 1 | 家属登录 | 集成测试 step3 | `Tests run: 21, Failures: 0` |
| 2 | 老人档案列表/新增 | 集成测试 step6 | `elderlyId=1` created via DataInitializer |
| 3 | 费用试算 | 集成测试 step7 | `totalAmount=100.00 (BASIC×2h)` |
| 4 | 下单(空格日期) | 集成测试 step8 | `orderNo=OD20261029...` @JsonFormat accepted |
| 5 | 订单列表 | API GET /api/order/page | 集成测试覆盖 |
| 6 | 生成告知书 | Mock AI LLM:9002 Node直连 | HTTP 200, 675 chars HTML `诊前就诊告知书` |
| 7 | 查看告知书 | Mock AI LLM:9002 Node直连 | `status=200, len=675, preview=<h3>诊前就诊告知书</h3>` |
| 8 | 通知列表 | API GET /api/notification/page | 集成测试 step19 覆盖 |

### 陪诊师端

| # | 项目 | 方法 | 证据 |
|---|---|---|---|
| 9 | 陪诊师登录 | 集成测试 step4 | `escortUserId=3, token non-null` |
| 10 | 接单池→接单 | 集成测试 step9-step10 | `POST /api/order/{id}/accept → 200` |
| 11 | 开始/结束服务 | 集成测试 step11-step13 | `IN_SERVICE → SERVICE_FINISHED → 200` |
| 12 | OCR识别 | Mock AI OCR:9001 实测 | HTTP 200, `result`字段存在, `confidence=0.95/0.72` |
| 13 | 待审核账号 | DataInitializer | `13800003333 audit_status=PENDING` |

### 管理端

| # | 项目 | 方法 | 证据 |
|---|---|---|---|
| 14 | 管理员登录 | 集成测试 step1 | `adminToken non-null` |
| 15 | 派单 | 集成测试 step9 | `POST .../dispatch?escortId=3 → 200` |
| 16 | 资质审核详情 | 集成测试 step5 | `idCard=18位解密明文, 330102****1234` |
| 17 | AI内容复核 | API PUT /api/ai/content/{id}/review | 集成测试覆盖 |

### AI专项

| # | 项目 | 方法 | 证据 |
|---|---|---|---|
| 18 | Mock OCR SUCCESS | 直连:9001 | HTTP 200, result对象完整, confidence交替0.95/0.72 |
| 19 | Mock LLM SUCCESS | Node.js直连:9002 | HTTP 200, 675 chars, `<h3>诊前就诊告知书</h3>` |
| 20 | 重试机制验证 | Mock:9002 500触发 | HTTP 500 returned, 后端重试→降级 |
| 21 | 构建回归 | mvn + npm | COMPILE OK + 21/21 BUILD SUCCESS + built 5.42s |

### 关键修复验证

| BUG | 修复 | 验证 |
|---|---|---|
| BUG-A: id=null链路断裂 | `degraded()` 落库 | 降级时 `contentMapper.insert(c)` → 返回真实id |
| BUG-B: AI服务缺失 | `mock-ai-server/server.js` | 9001/9002 双端口监听, POST 200 |
| BUG-C: prompt null | `"未填写"/"无"/"未填写"` 替代空字符串 | prompt中不再出现"null"文字 |
| BUG-D: 前端跳转防御 | `typeof id === 'number'` 校验 | 非数字id提示"请从就医档案列表查看" |