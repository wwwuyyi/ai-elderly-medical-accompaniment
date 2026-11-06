# 项目交接档案（新模型接管必读）

> 本文档是本项目唯一权威的进度与约束交接依据。你是接手本项目的新模型，必须先读完本文档与《DeepSeek_毕设项目提示词.md》（位于项目上级目录），输出《接管确认书》后等待用户指令，禁止立即动手改代码。

> 2026-11-06 联调整改更新：T-09 文档更新已交付，待管理方逐字验收；21 项必测原始证据索引见第十节。

## 一、项目概况

- 课题：面向老年群体AI赋能陪诊助医服务系统（本科毕业设计）
- 技术栈（已锁定）：JDK 17语法 / SpringBoot 3.2.5 / MyBatis-Plus 3.5.5 / MySQL 8.0 / jjwt 0.12.5 / Knife4j 4.5.0 / OkHttp 4.12.0 / Hutool 5.8.25（BCrypt走Hutool）/ 前端 Vue 3.4 + Vite 5 + Element Plus 2.x + Pinia + Axios
- 明确不引入：Redis / SpringSecurity / Sentinel / MQ / OSS / WebSocket
- 项目根目录：`C:\Users\admin\Desktop\AI赋能陪诊助医服务系统\ai-elderly-medical-accompaniment`，前端在 `frontend/` 子目录
- 全部业务数据为自行构造的模拟数据（医院已虚构化），不涉及真实医疗数据
- **Mock AI Server**：项目含 `mock-ai-server/server.js`（零依赖 Node.js），监听 9001(OCR) / 9002(LLM)，启动命令：`node mock-ai-server/server.js`。OCR 交替返回高置信度(0.95)/低置信度(0.72)；LLM 按 prompt 类型返回中文 HTML 告知书/解读内容——所有医疗文案为通用就诊常识，不构成诊疗建议。

## 二、进度坐标

已完成：P0需求确认 → P1数据库（初始13表+10家虚构医院；现行15表见快照）→ P2后端骨架 → P3认证与用户域 → P4订单闭环（状态机/乐观锁/幂等）→ P5档案与脱敏 → P6 AI模块（九项机制）→ P7管理端 → P8前端三端（30页面，含脱敏预览页）→ **P9测试设计（功能用例 53（HANDOFF 历史值）／54（11-02 论文最终口径，含 TC-AI-008）【待核，管理方未实测用例清单】+21集成测试+JMeter方案+18安全用例）已收官**

**当前阶段：P10 论文初稿 —— 进行中（P10a：摘要/绪论/相关技术介绍）**

下一阶段：P10b（需求分析/系统设计）→ P10c（系统实现/测试/总结）→ P11 修改定稿

## 三、关键设计定稿（不得推翻）

1. 订单状态机：8状态（PENDING_DISPATCH→DISPATCHED→ACCEPTED→IN_SERVICE→SERVICE_FINISHED→SETTLED→COMPLETED + CANCELLED），6进行态+2终态，10条流转规则，全部状态变更走乐观锁条件UPDATE（WHERE status+version，影响行数0抛409）
2. 订单号：OD+yyyyMMdd+6位序列，insert纳入重试循环，DuplicateKeyException按约束名分支（uk_order_no重试3次 / uk_request_id幂等返回）
3. 支付模型：下单即模拟支付（pay_status=PAID）；T9取消全额退、T10取消退80%（cancel.refund-rate=0.8，距预约≥2h+必填原因）；IN_SERVICE后不可取消
4. 计价：base=50元/h、exam=80元/h（application.yml配置，不足1h按1h）；平台抽成20%
5. 预约时间：escort_order.appointment_time DATETIME（T5开始服务校验±30分钟窗口）
6. 脱敏：@SensitiveField+Jackson序列化器，FAMILY全量/ADMIN脱敏/ESCORT仅电话全量
7. AI：OkHttp双客户端（OCR 15s / LLM 30s）、重试仅超时+5xx+429（AiApiException带statusCode）、SimpleCircuitBreaker熔断器（@Bean双实例）、置信度0.85阈值、ai_task审计（SHA-256摘要禁明文）、免责声明强制落库
8. 管理员：admin/admin123由DataInitializer初始化（BCrypt），登录后弹窗提示改密
9. 端口：后端8080，前端5173，/api代理

## 四、违规登记（新模型引以为戒）

1. 第一次严重违规：医院模拟数据使用真实医院名称/地址/电话（已修复）
2. 第二次严重违规：P6误删pom.xml中Lombok依赖导致编译失败，虚构"JDK21不兼容"说法，并执行delombok生成103个重复文件（已回滚）
3. 第三次不实表述："JDK21 runtime阻断"（实为LambdaUpdateWrapper回归，已修复）
4. 反复出现：计数错误（阶段数/功能数/索引数/文件数/接口数累计6次）；"已完成/已落盘/正式完成"等无验证背书的表述
5. 第四次违规（2026-10-28，事后复核撤销）：《接管确认书》中"27处双/api前缀仍存在"的表格经用户侧正则复核，行号与数据全部属实，系用户端正则未覆盖模板字符串变体导致的误判。撤销。新模型接管的核实方法论有效。
6. V-01（2026-11-06 追加）：T-07 回执称“上一轮已交付、本轮零改动”，与 `mock-ai-server/evidence-t07-admin.js` 的 mtime 19:44:11 证据不符。事实登记，待管理方核定。

**生效中的新规矩：任何"完成/已修复"表述必须附验证证据（构建输出/接口测试截图/自测日志），否则视为未交付。**

## 五、P8/P9 完成状态与项目快照（2026-11-06 更新）

**P8 前端三端 —— 已收官（2026-10-28 历史记录）**：
- 27处模板字符串双/api前缀清零（14文件）
- 4处前端JSON body→params传参修正（audit/toggleUser/hideEvaluation/dispatch）
- 新增管理端档案脱敏预览页 `admin/RecordPreview.vue`
- 新增H2 dev profile（`-Pdev -Dspring-boot.run.profiles=dev`）
- OrderNoGenerator重写为MyBatis-Plus标准查询（MySQL/H2双兼容）
- npm run build ✓ + 21步全流程集成测试 BUILD SUCCESS

**P9 测试设计 —— 已收官（2026-10-28 历史记录）**：
- 功能测试：53条用例（认证7+老人7+陪诊6+订单11+档案6+AI 7+管理6+通知3）
- 性能测试：JMeter 5场景×3阶梯并发方案（结果留白）
- 安全测试：18条（水平越权3+垂直越权3+未授权3+SQL注入2+XSS 2+脱敏5）
- 新增审核详情端点 `GET /api/admin/escort/audit/{id}/detail`（AES解密idCard返回明文）

**项目快照（2026-11-06 管理方实测）**：

| 指标 | 实测值 | 计数方式 |
|---|---|---|
| 数据库表 | 15 | information_schema.TABLES（escort_db） |
| 二级索引 | 43 | distinct TABLE.INDEX 共 58 − 15 个 PRIMARY |
| UNIQUE 二级索引 | 7 | NON_UNIQUE=0 共 22 − 15 个 PRIMARY |
| Controller | 12 | grep -rl '@RestController' 命中 13，扣除 GlobalExceptionHandler.java（实为 @RestControllerAdvice 前缀误匹配） |
| API 端点 | 67 | @Get/Post/Put/Delete/PatchMapping 全局计数 67；逐类求和 13+9+8+7+6+5+5+5+4+3+1+1=67（双算法互证） |
| Java 主源 / 测试 | 127 / 2 | find src/main/java -name '*.java'、find src/test -name '*.java' |
| 前端 Vue 页面 | 30 | find frontend/src/views -name '*.vue' |
| 前端路由 | 35 处 path 声明 | router/index.js；扣除根路径 `/` redirect 1 处后为 34 条业务路由（无通配路由） |
| SQL 脚本 | 5 | 01-schema / 02-init-data / 03-alter-order-request-id / 04-add-chat-tables / 05-fix-gender-width |
| mock-ai-server 脚本 | 12 | server.js、self-test.js、retry-fixture.js、env-probe.js、evidence-family.js、evidence-degraded.js、evidence-sql.js、evidence-retry.js、evidence-t05-family.js、evidence-t05-degraded.js、evidence-t06-escort.js、evidence-t07-admin.js |
| 集成测试 | 21 步全绿 | Tests run: 21, Failures: 0, Errors: 0, Skipped: 0（BUILD SUCCESS 8.222 s） |
| 构建 | mvn compile BUILD SUCCESS（5.078 s / 15.752 s）；npm run build ✓ built in 5.19s | 原始输出留存 |
| 订单状态机 | 8 状态 | 实测走过 PENDING_DISPATCH→DISPATCHED→ACCEPTED→IN_SERVICE→SERVICE_FINISHED 与 CANCELLED；version 乐观锁实测 0→1→2→3→4 |
| AI 审计 | ai_task 20 行：SUCCESS 5 / FAILED 7 / DEGRADED 8 | response_digest 与响应体独立计算 SHA-256 前16位一致（fca86ad7f53d72b3） |

**前端路由 35 处 path 明细（frontend/src/router/index.js）**：

1. `/login`
2. `/register`
3. `/family/home`
4. `/family/elderly`
5. `/family/elderly/add`
6. `/family/elderly/:id`
7. `/family/order/create`
8. `/family/orders`
9. `/family/order/:id`
10. `/family/evaluate/:id`
11. `/family/records`
12. `/family/record/:id`
13. `/family/notice/:id`
14. `/family/ai-chat`
15. `/family/ai-chat/:sessionId`
16. `/escort/home`
17. `/escort/apply`
18. `/escort/pool`
19. `/escort/orders`
20. `/escort/order/:id`
21. `/escort/ocr/:taskId`
22. `/escort/statistics`
23. `/escort/profile`
24. `/family/profile`
25. `/admin/profile`
26. `/admin/dashboard`
27. `/admin/audit`
28. `/admin/users`
29. `/admin/orders`
30. `/admin/evaluations`
31. `/admin/record-preview`
32. `/family/notify`
33. `/escort/notify`
34. `/admin/notify`
35. `/`（redirect: `/login`）

说明：35 处 path 声明中，`/` 为根路径重定向，扣除后为 34 条业务路由；文件中未发现通配路由（如 `/:pathMatch(.*)*`），与论文“34 条业务路由”口径一致。

**新增API接口（P8/P9 历史记录，最新计数见上方 2026-11-06 快照）**：
- `GET /api/hospital/list` — 医院字典列表（登录可访问）
- `GET /api/admin/escort/audit/{escortUserId}/detail` — 审核详情（含解密idCard明文，仅ADMIN）

## 六、工作协议摘要（完整版见提示词原文）

- 分阶段交付：P0→P11严格按序，每阶段结束输出交付物清单+待确认清单+《项目交接快照》
- 反幻觉铁律：禁止编造不存在的技术/版本/文献/医学内容；禁止虚构厂商API细节；不确定必须标注；数字必须可复核
- 所有AI生成医疗内容必须展示「AI生成内容仅供参考，不构成任何诊疗建议」
- 用户回复"继续"=验收通过；打回意见必须逐条落实

## 七、四进程启动与建库顺序（2026-11-06 实测口径）

### 7.1 四进程启动步骤

1. **MySQL（3306）**

```bash
"D:/MySQL/mysql-8.0.40-winx64/bin/mysqld.exe" --defaults-file=D:/MySQL/my.ini
```

2. **Mock AI（9001 OCR / 9002 LLM）**

```bash
node mock-ai-server/server.js
```

3. **后端（8080）**

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--ai.llm.base-url=http://localhost:9002/llm --ai.ocr.base-url=http://localhost:9001/ocr --ai.api-key=demo-api-key"
```

4. **前端（5173）**

```bash
cd frontend && npm run dev
```

注意：vite 实际绑定 `::1`，IPv4 `127.0.0.1:5173` 探测会失败，须用 `http://localhost:5173`。

**存活校验：**

```bash
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/captcha
# 期望输出：200
netstat -ano | grep -E ":3306|:8080|:9001|:9002|:5173" | grep LISTEN
```

### 7.2 建库顺序

按序执行 5 个脚本：

1. `sql/01-schema.sql`
2. `sql/02-init-data.sql`
3. `sql/03-alter-order-request-id.sql`
4. `sql/04-add-chat-tables.sql`
5. `sql/05-fix-gender-width.sql`

旧库升级必须补跑第 5 个（`sql/05-fix-gender-width.sql`），否则女性数据写入会 500。

## 八、AI 地址口径与取证纪律（2026-11-06）

### 8.1 AI 地址口径

- `application.yml` 默认 `ai.llm.base-url=https://api.deepseek.com/v1`（真实厂商）、`ai.ocr.base-url=http://localhost:9001/ocr`。
- 用 mock 演示必须按 7.1 的后端命令覆盖注入；否则 LLM 全部走降级。**【D-02 待管理方裁定】**

### 8.2 取证纪律（三条）

1. 改 `mock-ai-server/server.js` 或后端代码后，必须重启对应进程。陈旧进程会产出假证据（本轮两次命中）。
2. 中文请求体禁止 Git Bash `curl` 直传（按 GBK 传参会造成关键词不命中的假阴性）；必须用 Node UTF-8 客户端或 `curl --data-binary @utf8文件`。
3. mysql 客户端不在 PATH，必须用绝对路径；`evidence-sql.js` 已按 `MYSQL_BIN` → 绝对路径 `D:/MySQL/mysql-8.0.40-winx64/bin/mysql.exe` → PATH 三级解析。

## 九、本轮修复登记（2026-11-06）

| 编号 | 修复内容 | 指向文件与改法 |
|---|---|---|
| BUG-E | 全系统写入 500（create_time cannot be null） | 新增 `src/main/java/com/example/escort/config/MyMetaObjectHandler.java`：insertFill 填充 createTime + updateTime；updateFill 刷新 updateTime；值 LocalDateTime.now() |
| BUG-F | 文件上传路径缺陷 | `src/main/java/com/example/escort/module/file/controller/FileController.java` 的 `upload()`：目标文件改为 `new File("./upload/" + newFileName).getAbsoluteFile()` |
| BUG-G | gender 列宽与 MALE/FEMALE 不兼容 | 新增 `sql/05-fix-gender-width.sql`；`sql/01-schema.sql` 第 44/71 行、`src/main/resources/schema-h2.sql` 两表 gender → `VARCHAR(10) NOT NULL` |
| BUG-H | updateFill 用 strictUpdateFill 致实体更新路径不刷新 update_time | **【待授权，未修】** |

BUG-A/B/C/D 的验证证据见第十节 21 项必测索引，本轮未新增代码改动。

## 十、21 项必测闭环结论与证据索引（2026-11-06）

21 项必测闭环（管理方 2026-11-06 代跑已取得原始输出；证据索引见下）。最终验收以管理方逐项复核为准。脚本清单见第五节快照；原始日志由管理方留存于 `/tmp`。

| 必测 | 覆盖内容 | 取证脚本 | 原始日志 |
|---|---|---|---|
| 1-6 | 家属端登录/档案/试算下单/告知书/取消/通知 | `mock-ai-server/evidence-t05-family.js` | `/tmp/t05-03-family.log` |
| 7-10 | 陪诊端登录/接单流转/OCR/认证申请 | `mock-ai-server/evidence-t06-escort.js` | `/tmp/t06c-escort.log` |
| 11-15 | 管理端登录/派单/资质审核/AI复核/用户列表 | `mock-ai-server/evidence-t07-admin.js` | `/tmp/t07-admin.log` |
| 16 | ai_task / ai_generated_content SQL 审计 | `mock-ai-server/evidence-sql.js` | `/tmp/t06c-escort.log`（必测9 ai_task OCR 行 SQL 段）+ 管理方 mysql 客户端输出（未落盘；原始输出见管理方复核记录，如需归档由管理方补存） |
| 17 | mock 停止态降级 + 熔断阈值 | `mock-ai-server/evidence-t05-degraded.js` | `/tmp/t05-04-degraded.log` |
| 18 | 500 指数退避重试 / 非 5xx 不重试 | `mock-ai-server/evidence-retry.js` | 管理方复核记录，未落盘；如需归档由管理方补存（retry_count=3 / cost_ms=7031 / HTTP 500；retry_count=0 / cost_ms=13 / HTTP 400，夹具仅 1 次请求） |
| 19-21 | mvn compile / FullFlowIntegrationTest / npm run build | 构建命令原始输出 | 21/21、5.078 s / 15.752 s、5.19 s |

环境前置证据：`mock-ai-server/env-probe.js` → `/tmp/t05-01-env-probe.log`。

## 十一、待裁定与登记事项清单（2026-11-06，只写事实，不写结论）

1. **D-01 退款口径**：管理方实测 PENDING_DISPATCH / DISPATCHED 取消为全额退款，ACCEPTED 取消为 80%（0.8）；与打回单必测5 的表述不符。**【待管理方裁定】**
2. **D-02 LLM 默认地址**：`application.yml` 默认 `ai.llm.base-url=https://api.deepseek.com/v1`（真实厂商）、`ai.ocr.base-url=http://localhost:9001/ocr`；mock 演示需按 7.1 命令覆盖注入，否则 LLM 全部走降级。**【待管理方裁定】**
3. **D-03 SensitiveConfigWarnLogger**：启动已实证输出 3 条安全警告；类来源与登记方式待确认。**【待管理方裁定】**
4. **D-05 11-05 未登记改动**：`config` 目录 mtime 显示 `WebMvcConfig.java` 19:09、`SensitiveConfigWarnLogger.java` 19:10；项目无 Git，未见可读 Local History，无法仅凭文件系统还原改动内容。**【待管理方裁定】**
5. **D-06 通知死代码**：全项目检索 `NotifyService.send(...)` 零调用方；家属通知列表 `total=0` 为当前实现事实行为。**【待管理方裁定】**
6. **F-07 明文 Key**：`start-deepseek.bat` 内存在明文真实 DeepSeek Key（本文件不抄录 Key 值），属实际泄露，需立即吊销/轮换。**【待管理方裁定】**
7. **OBS-02**：H2 与 MySQL schema 差异（gender 宽度已按 T-12 对齐至 `VARCHAR(10)`）；其余差异待核。**【待管理方裁定】**
8. **OBS-03**：枚举码中文展示问题，本轮未修改。**【待管理方裁定】**
9. **OBS-05 / OBS-06**：
   - **OBS-05**：FullFlowIntegrationTest 走 H2 dev profile，未覆盖文件上传与 gender=FEMALE 分支，致 BUG-E/F/G 三个 P0 缺陷在「21 步全绿」下漏网；建议论文测试章节据实说明并补 MySQL profile 回归。
   - **OBS-06**：越权访问审核详情返回传输层 HTTP 200 + 业务码 403「角色权限不足」，而未登录场景返回 HTTP 401；拦截有效、无数据泄露，但 HTTP 语义不一致，网关/监控按状态码判定会误判为成功。
   **【待管理方裁定】**
10. **V-01**：T-07 回执称“上一轮已交付、本轮零改动”，与 `mock-ai-server/evidence-t07-admin.js` 的 mtime 19:44:11 证据不符；事实登记，不作结论。**【待管理方裁定】**
11. **测试数据清单（2026-11-06 现场）**：`elderly_profile` id=3/4；`escort_profile` id=2；`escort_order` id=1-13；`medical_record` id=1-2；`medical_document` id=1-2；`ai_task` 20 行；`ai_generated_content` 9 行；`upload/e3434c2c…png`。清理方案待定。**【待管理方裁定】**
12. **T-13**：管理方后续待定事项，本文件未获具体内容，不臆测。**【待管理方裁定】**