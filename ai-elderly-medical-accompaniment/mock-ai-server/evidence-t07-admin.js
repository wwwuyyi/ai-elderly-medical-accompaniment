// T-07 实测脚本（管理端必测 11-15）
// 用法（后端 8080、mock 9001/9002、MySQL 已启动）：
//   node mock-ai-server/evidence-t07-admin.js
// 说明：仅使用 Node 内置模块（fetch），无参数；所有中文请求体由 Node UTF-8 发送。
//       本脚本不执行任何 SQL；数据变更全部走既有 API（建单/派单/审核/复核）。
//
// ===== 端点源码依据（逐项，文件:行号） =====
// 必测11 登录：AuthController.java:25 @RequestMapping("/api/auth")，:49 @PostMapping("/login")
// 必测12 建单：EscortOrderController.java:36-40 POST /api/order（@RequireRole("FAMILY")）
//        派单：EscortOrderController.java:66-70 POST /api/order/{id}/dispatch?escortId=（@RequireRole("ADMIN")）
//        回查：EscortOrderController.java:52-55 GET /api/order/{id}
//        version：OrderVO 无 version 字段，改由 AdminController.java:50-54
//                 GET /api/admin/order/page?keyword= 返回 EscortOrder 实体（含 version）回查
// 必测13 列表：AdminController.java:27-30 GET /api/admin/escort/audit/page（query current/size/auditStatus）
//        详情：AdminController.java:37-39 GET /api/admin/escort/audit/{escortUserId}/detail
//              解密依据：AdminServiceImpl.java:80-98（aesUtil.decrypt）
//        审核：AdminController.java:32-34 POST /api/admin/escort/audit/{escortUserId}?status=&remark=（query 参数）
//        权限：AdminController.java:20 类级 @RequireRole("ADMIN")
// 必测14 复核：AiController.java:51-54 PUT /api/ai/content/{id}/review（body：reviewStatus/reviewRemark）
//        内容详情：AiController.java:46-49 GET /api/ai/content/{id}
// 必测15 用户：AdminController.java:41-44 GET /api/admin/user/page（query current/size/keyword）
//
// 备注：若任一请求实测 404/401，原样打印，不猜测路径重试后只报成功那次。

const BASE = 'http://127.0.0.1:8080';

function ts() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}
function fmtLocal(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
function shortToken(t) { return t ? t.slice(0, 16) + '...(' + t.length + ' chars)' : String(t); }
function out(label) { console.log(`\n===== ${label} =====`); }
function is18Digits(s) { return typeof s === 'string' && /^\d{18}$/.test(s); }

async function call(method, path, body, token, opts = {}) {
  const headers = { 'Content-Type': 'application/json; charset=utf-8' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const started = Date.now();
  const t0 = ts();
  const res = await fetch(BASE + path, {
    method, headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  const ms = Date.now() - started;
  let json = null;
  try { json = JSON.parse(text); } catch (e) {}
  console.log(`[${t0}] ${method} ${path}`);
  if (body !== undefined) console.log('  request body: ' + JSON.stringify(body));
  console.log(`  -> HTTP ${res.status}  (${ms} ms)`);
  const limit = opts.full ? 100000 : 8000;
  console.log('  body: ' + (text.length > limit ? text.slice(0, limit) + '...[truncated,total=' + text.length + ']' : text));
  return { status: res.status, text, json, ms, t0 };
}

async function login(username, password, role) {
  return call('POST', '/api/auth/login', {
    username, password, captcha: '0000', captchaKey: 'test-bypass', role
  });
}

async function getOrderDetail(orderId, token, label) {
  // 源码依据：EscortOrderController.java:52-55 GET /api/order/{id}
  const r = await call('GET', '/api/order/' + orderId, undefined, token);
  const d = (r.json && r.json.data) || {};
  console.log(`  [${label}] orderNo=${d.orderNo} status=${d.status} escortId=${d.escortId}` +
              ` escortName=${d.escortName}`);
  return d;
}

async function getOrderVersion(orderNo, adminToken, label) {
  // 源码依据：AdminController.java:50-54 GET /api/admin/order/page（返回 EscortOrder 实体，含 version）
  const r = await call('GET', '/api/admin/order/page?current=1&size=10&keyword=' + encodeURIComponent(orderNo),
    undefined, adminToken);
  const records = (r.json && r.json.data && r.json.data.records) || [];
  const rec = records.find(x => x.orderNo === orderNo) || null;
  console.log(`  [${label}] admin order page: id=${rec && rec.id} orderNo=${rec && rec.orderNo}` +
              ` status=${rec && rec.status} escortId=${rec && rec.escortId} version=${rec && rec.version}`);
  return rec;
}

(async () => {
  // ========== 必测11：管理端登录 ==========
  out('必测11 管理端登录 POST /api/auth/login（admin/admin123/ADMIN）');
  let r = await login('admin', 'admin123', 'ADMIN');
  const adminToken = r.json && r.json.data && r.json.data.token;
  const adminUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) +
              ' data.role=' + (r.json && r.json.data && r.json.data.role) +
              ' data.userId=' + adminUserId +
              ' token.length=' + (adminToken ? adminToken.length : 'null') +
              ' token=' + shortToken(adminToken));
  if (!adminToken) throw new Error('管理员登录失败，停止后续步骤');

  // ========== 前置：家属/陪诊师登录（建单与越权负例） ==========
  out('前置：家属登录（13800001111）');
  r = await login('13800001111', 'Test1234', 'FAMILY');
  const familyToken = r.json && r.json.data && r.json.data.token;
  const familyUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' userId=' + familyUserId +
              ' token=' + shortToken(familyToken));
  if (!familyToken) throw new Error('家属登录失败，停止后续步骤');

  out('前置：陪诊师登录（13800002222）');
  r = await login('13800002222', 'Test1234', 'ESCORT');
  const escortToken = r.json && r.json.data && r.json.data.token;
  const escortUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' userId=' + escortUserId + ' token=' + shortToken(escortToken));
  if (!escortToken) throw new Error('陪诊师登录失败，停止后续步骤');

  // ========== 必测12：家属建单 -> admin 派单 -> 回查 ==========
  out('必测12 家属建单 -> admin 派单 -> 回查（必测12 归档）');
  // 源码依据：ElderlyProfileController.java:57-65 GET /api/elderly/page（@RequireRole("FAMILY")）
  r = await call('GET', '/api/elderly/page?current=1&size=10', undefined, familyToken);
  const elderlyId = r.json && r.json.data && r.json.data.records && r.json.data.records[0] && r.json.data.records[0].id;
  console.log('  使用老人档案 id=' + elderlyId);
  if (!elderlyId) throw new Error('无老人档案，停止必测12');

  // 源码依据：EscortOrderController.java:29-33 POST /api/order/estimate（登录即可）
  r = await call('POST', '/api/order/estimate', { serviceType: 'BASIC', duration: 2 }, familyToken);
  const amount = r.json && r.json.data && r.json.data.totalAmount;
  console.log('  试算 totalAmount=' + amount);
  if (amount === undefined || amount === null) throw new Error('费用试算失败，停止必测12');

  const appt = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  r = await call('POST', '/api/order', {
    requestId: 'T07-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId, hospitalId: 1, department: '心内科',
    serviceType: 'BASIC', appointmentTime: appt, duration: 2, amount
  }, familyToken);
  const order = (r.json && r.json.data) || {};
  console.log('  order.id=' + order.id + ' orderNo=' + order.orderNo +
              ' status=' + order.status + ' amount=' + order.amount +
              ' appointmentTime=' + order.appointmentTime);
  if (!order.id) throw new Error('订单创建失败，停止必测12');

  out('必测12a 派单前回查 GET /api/order/{id}');
  const beforeDetail = await getOrderDetail(order.id, familyToken, '派单前');
  out('必测12b 派单前 version 回查 GET /api/admin/order/page?keyword=' + order.orderNo);
  const beforeEntity = await getOrderVersion(order.orderNo, adminToken, '派单前');

  out('必测12c admin 派单 POST /api/order/{id}/dispatch?escortId=' + escortUserId);
  await call('POST', '/api/order/' + order.id + '/dispatch?escortId=' + escortUserId, undefined, adminToken);

  out('必测12d 派单后回查 GET /api/order/{id}');
  const afterDetail = await getOrderDetail(order.id, familyToken, '派单后');
  out('必测12e 派单后 version 回查 GET /api/admin/order/page?keyword=' + order.orderNo);
  const afterEntity = await getOrderVersion(order.orderNo, adminToken, '派单后');

  console.log('\n  必测12 关键字段汇总：');
  console.log('  orderNo=' + order.orderNo +
              ' status(前)=' + (beforeDetail.status || (beforeEntity && beforeEntity.status)) +
              ' status(后)=' + (afterDetail.status || (afterEntity && afterEntity.status)) +
              ' escortId(后)=' + afterDetail.escortId +
              ' escortName(后)=' + afterDetail.escortName +
              ' version(前)=' + (beforeEntity && beforeEntity.version) +
              ' version(后)=' + (afterEntity && afterEntity.version));

  // ========== 必测13：资质审核（列表密文 / 详情明文 / 审核动作 / 越权负例） ==========
  out('必测13 资质审核：分页列表 GET /api/admin/escort/audit/page?auditStatus=PENDING');
  r = await call('GET', '/api/admin/escort/audit/page?current=1&size=10&auditStatus=PENDING',
    undefined, adminToken);
  let records = (r.json && r.json.data && r.json.data.records) || [];
  let target = records.find(x => x.userId === 4) ||
               records.find(x => x.realName === '王芳') ||
               records[0] || null;

  if (!target) {
    out('必测13 未发现 PENDING 记录，回查全量列表定位 user_id=4');
    r = await call('GET', '/api/admin/escort/audit/page?current=1&size=20', undefined, adminToken);
    records = (r.json && r.json.data && r.json.data.records) || [];
    target = records.find(x => x.userId === 4) || null;
  }

  const auditUserId = target && target.userId;
  console.log('  目标陪诊师: profileId=' + (target && target.id) +
              ' userId=' + auditUserId +
              ' realName=' + (target && target.realName) +
              ' gender=' + (target && target.gender) +
              ' auditStatus=' + (target && target.auditStatus));
  console.log('  列表 idCard 实值=' + (target && target.idCard) +
              '  (length=' + (target && target.idCard ? target.idCard.length : 0) +
              ', 18位数字=' + is18Digits(target && target.idCard) + ')');

  if (!auditUserId) throw new Error('未找到审核目标（user_id=4 / 王芳），停止必测13');

  out('必测13a 审核详情 GET /api/admin/escort/audit/' + auditUserId + '/detail（ADMIN）');
  r = await call('GET', '/api/admin/escort/audit/' + auditUserId + '/detail', undefined, adminToken);
  const detailBefore = (r.json && r.json.data) || {};
  console.log('  详情 idCard 实值=' + detailBefore.idCard +
              '  (length=' + (detailBefore.idCard ? detailBefore.idCard.length : 0) +
              ', 18位数字=' + is18Digits(detailBefore.idCard) + ')');
  console.log('  详情 auditStatus=' + detailBefore.auditStatus +
              ' auditorId=' + detailBefore.auditorId +
              ' auditTime=' + detailBefore.auditTime);

  out('必测13b 越权负例：FAMILY token 访问审核详情（原样输出，以实测为准）');
  await call('GET', '/api/admin/escort/audit/' + auditUserId + '/detail', undefined, familyToken);

  out('必测13c 越权负例：ESCORT token 访问审核详情（原样输出，以实测为准）');
  await call('GET', '/api/admin/escort/audit/' + auditUserId + '/detail', undefined, escortToken);

  const canApprove = target && target.auditStatus === 'PENDING';
  if (!canApprove) {
    console.log('\n  目标 auditStatus=' + (target && target.auditStatus) +
                '，非 PENDING，跳过审核通过动作（可能已在前次运行中审核）');
  } else {
    out('必测13d 审核通过 POST /api/admin/escort/audit/' + auditUserId + '?status=APPROVED&remark=...');
    console.log('  request query: status=APPROVED&remark=T07管理端审核通过');
    await call('POST', '/api/admin/escort/audit/' + auditUserId +
      '?status=APPROVED&remark=' + encodeURIComponent('T07管理端审核通过'), undefined, adminToken);

    out('必测13e 审核后详情回查 GET /api/admin/escort/audit/' + auditUserId + '/detail');
    r = await call('GET', '/api/admin/escort/audit/' + auditUserId + '/detail', undefined, adminToken);
    const detailAfter = (r.json && r.json.data) || {};
    console.log('  关键字段: auditStatus=' + detailAfter.auditStatus +
                ' auditRemark=' + detailAfter.auditRemark +
                ' auditorId=' + detailAfter.auditorId +
                ' auditTime=' + detailAfter.auditTime +
                ' idCard.length=' + (detailAfter.idCard ? detailAfter.idCard.length : 0));
  }

  // ========== 必测14：AI 内容复核（id=1 夹具） ==========
  out('必测14 AI 内容复核：复核前详情 GET /api/ai/content/1');
  r = await call('GET', '/api/ai/content/1', undefined, adminToken);
  const contentBefore = (r.json && r.json.data) || {};
  console.log('  复核前: id=' + contentBefore.id +
              ' contentType=' + contentBefore.contentType +
              ' reviewStatus=' + contentBefore.reviewStatus +
              ' reviewRemark=' + contentBefore.reviewRemark);

  if (!contentBefore.id) {
    console.log('  ai_generated_content id=1 不存在，跳过复核动作');
  } else {
    out('必测14a 提交复核 PUT /api/ai/content/1/review');
    await call('PUT', '/api/ai/content/1/review', {
      reviewStatus: 'REVIEWED',
      reviewRemark: 'T07管理端复核通过'
    }, adminToken);

    out('必测14b 复核后详情 GET /api/ai/content/1');
    r = await call('GET', '/api/ai/content/1', undefined, adminToken);
    const contentAfter = (r.json && r.json.data) || {};
    console.log('  复核后: id=' + contentAfter.id +
                ' contentType=' + contentAfter.contentType +
                ' reviewStatus=' + contentAfter.reviewStatus +
                ' reviewRemark=' + contentAfter.reviewRemark);
    console.log('  对照: reviewStatus ' + contentBefore.reviewStatus + ' -> ' + contentAfter.reviewStatus +
                ' ; reviewRemark ' + JSON.stringify(contentBefore.reviewRemark) +
                ' -> ' + JSON.stringify(contentAfter.reviewRemark));
  }

  // ========== 必测15：用户管理列表 ==========
  out('必测15 用户管理列表 GET /api/admin/user/page?current=1&size=10');
  r = await call('GET', '/api/admin/user/page?current=1&size=10', undefined, adminToken);
  const userPage = (r.json && r.json.data) || {};
  const userRecords = userPage.records || [];
  const firstUser = userRecords[0] || null;
  console.log('  total=' + userPage.total + ' records=' + userRecords.length);
  console.log('  首行记录（接口原样返回，未加工）=' + JSON.stringify(firstUser));

  console.log('\n===== 关键ID汇总 =====');
  console.log(JSON.stringify({
    adminUserId,
    familyUserId,
    escortUserId,
    orderId: order.id,
    orderNo: order.orderNo,
    statusBefore: beforeDetail.status,
    statusAfter: afterDetail.status,
    versionBefore: beforeEntity && beforeEntity.version,
    versionAfter: afterEntity && afterEntity.version,
    auditUserId,
    aiContentId: 1
  }, null, 2));
})().catch(e => { console.error('实测脚本异常: ' + (e && e.stack || e)); process.exit(1); });