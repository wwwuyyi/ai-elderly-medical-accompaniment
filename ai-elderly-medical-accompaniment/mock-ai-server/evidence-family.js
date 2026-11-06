// T-05 实测脚本（mock 运行态）：必测 1-6 + 必测 16(SUCCESS行) + 必测 18(500重试)
// 用法（后端 8080、mock 9001/9002、MySQL 均已启动）：
//   node mock-ai-server/evidence-family.js
// 说明：仅使用 Node 内置能力，不修改任何业务代码。

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
function shortToken(t) { return t ? t.slice(0, 24) + '...(' + t.length + ' chars)' : String(t); }
function out(label, extra) { console.log(`\n===== ${label} =====${extra ? ' ' + extra : ''}`); }

const state = {};

async function call(label, method, path, body, token, opts = {}) {
  const headers = { 'Content-Type': 'application/json' };
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
  const limit = opts.full ? 100000 : 2500;
  console.log('  body: ' + (text.length > limit ? text.slice(0, limit) + '...[truncated,total=' + text.length + ']' : text));
  return { status: res.status, text, json, ms, t0 };
}

async function main() {
  // ========== 必测1：家属登录 ==========
  out('必测1 家属登录 POST /api/auth/login');
  let r = await call('login', 'POST', '/api/auth/login', {
    username: '13800001111', password: 'Test1234',
    captcha: '0000', captchaKey: 'test-bypass', role: 'FAMILY'
  });
  state.familyToken = r.json && r.json.data && r.json.data.token;
  console.log('  token(first24)=' + shortToken(state.familyToken) +
              '  code=' + (r.json && r.json.code) +
              '  data.role=' + (r.json && r.json.data && r.json.data.role));
  if (!state.familyToken) throw new Error('家属登录失败，停止后续步骤');

  // ========== 必测2：老人档案列表 + 新增 ==========
  out('必测2a 老人档案列表 GET /api/elderly/page?current=1&size=10');
  r = await call('elderly list', 'GET', '/api/elderly/page?current=1&size=10', undefined, state.familyToken);
  const list = (r.json && r.json.data && r.json.data.records) || [];
  console.log('  total=' + (r.json && r.json.data && r.json.data.total) +
              '  first.id=' + (list[0] && list[0].id) +
              '  first.name=' + (list[0] && list[0].name));
  state.elderlyId = list[0] && list[0].id;

  out('必测2b 新增老人档案 POST /api/elderly');
  const elderName = 'T05测试老人' + Date.now().toString().slice(-6);
  const elderBody = {
    name: elderName, gender: 'MALE', birthDate: '1952-06-18',
    idCard: '330102195206181234', phone: '13900002222',
    address: '广州市测试路T05号', emergencyContact: '13800001111',
    emergencyName: '测试家属', mobility: 'ASSIST',
    allergyHistory: '青霉素过敏', chronicDisease: '高血压', remark: 'T05实测新增'
  };
  r = await call('elderly create', 'POST', '/api/elderly', elderBody, state.familyToken);
  console.log('  create response code=' + (r.json && r.json.code) + ' data=' + JSON.stringify(r.json && r.json.data));

  r = await call('elderly page after create', 'GET', '/api/elderly/page?current=1&size=20', undefined, state.familyToken);
  const newList = (r.json && r.json.data && r.json.data.records) || [];
  const created = newList.find(x => x.name === elderName);
  state.elderlyId = created ? created.id : state.elderlyId;
  console.log('  新增回查 name=' + elderName + ' 真实id=' + (created && created.id));

  // ========== 必测3：试算 -> 下单 -> 订单列表 ==========
  out('必测3a 费用试算 POST /api/order/estimate');
  r = await call('estimate', 'POST', '/api/order/estimate', { serviceType: 'BASIC', duration: 2 }, state.familyToken);
  const amount = r.json && r.json.data && r.json.data.totalAmount;
  console.log('  totalAmount=' + amount + ' unitPrice=' + (r.json && r.json.data && r.json.data.unitPrice));

  out('必测3b 下单 POST /api/order（appointmentTime 空格格式）');
  const appt = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  const orderBody = {
    requestId: 'T05-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '心内科',
    serviceType: 'BASIC', appointmentTime: appt, duration: 2, amount: amount
  };
  r = await call('create order', 'POST', '/api/order', orderBody, state.familyToken);
  state.orderId = r.json && r.json.data && r.json.data.id;
  state.orderNo = r.json && r.json.data && r.json.data.orderNo;
  console.log('  order.id=' + state.orderId + '  order.orderNo=' + state.orderNo +
              '  amount=' + (r.json && r.json.data && r.json.data.amount) +
              '  appointmentTime=' + (r.json && r.json.data && r.json.data.appointmentTime));

  out('必测3c 订单列表 GET /api/order/page?current=1&size=10');
  r = await call('order page', 'GET', '/api/order/page?current=1&size=10', undefined, state.familyToken);
  const orders = (r.json && r.json.data && r.json.data.records) || [];
  const found = orders.find(x => x.id === state.orderId);
  console.log('  列表中命中订单: id=' + (found && found.id) + ' orderNo=' + (found && found.orderNo) +
              ' status=' + (found && found.status) + ' amount=' + (found && found.amount));

  // ========== 必测4：生成告知书 -> 查看（mock 运行态） ==========
  out('必测4a 生成告知书 POST /api/ai/notice/generate（mock 运行态）');
  r = await call('notice generate', 'POST', '/api/ai/notice/generate', { orderId: state.orderId }, state.familyToken, { full: true });
  state.noticeId = r.json && r.json.data && r.json.data.id;
  const noticeContent = (r.json && r.json.data && r.json.data.content) || '';
  console.log('  data.id=' + state.noticeId + ' (type=' + typeof state.noticeId + ')');
  console.log('  data.contentType=' + (r.json && r.json.data && r.json.data.contentType));
  console.log('  data.content len=' + noticeContent.length +
              '  contains 降级文案="AI服务暂不可用" ? ' + noticeContent.includes('AI服务暂不可用') +
              '  contains 字符串null ? ' + noticeContent.includes('null'));
  console.log('  data.content 前300字符: ' + noticeContent.slice(0, 300));

  out('必测4b 查看告知书 GET /api/ai/content/{id}');
  r = await call('content get', 'GET', '/api/ai/content/' + state.noticeId, undefined, state.familyToken, { full: true });
  const got = (r.json && r.json.data) || {};
  console.log('  id=' + got.id + ' contentType=' + got.contentType +
              ' 内容一致=' + (got.content === noticeContent) +
              ' disclaimer=' + got.disclaimer);

  // ========== 必测5：取消规则两例 ==========
  // 5a PENDING_DISPATCH 取消（代码规则：全额）
  out('必测5a 新建订单并 PENDING_DISPATCH 取消');
  const appt2 = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  r = await call('create order for cancel A', 'POST', '/api/order', {
    requestId: 'T05-CA-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '内科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelAId = r.json && r.json.data && r.json.data.id;
  console.log('  orderA.id=' + cancelAId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo));
  r = await call('cancel A (PENDING_DISPATCH)', 'PUT', '/api/order/' + cancelAId + '/cancel', { cancelReason: 'T05测试-待派单取消' }, state.familyToken);
  const cancelAResp = r.text;
  r = await call('order A detail after cancel', 'GET', '/api/order/' + cancelAId, undefined, state.familyToken);
  console.log('  orderA status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount) +
              '  (cancel接口响应体=' + cancelAResp + ')');

  // 5b T10（ACCEPTED）取消（代码规则：退80%）
  // 同时实测 DISPATCHED 状态取消，用于核对打回单“DISPATCHED/T10 退80%”表述。
  out('必测5b 新建订单 -> 派单 -> 接单(ACCEPTED/T10) -> 取消');
  r = await call('create order for cancel B', 'POST', '/api/order', {
    requestId: 'T05-CB-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '外科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelBId = r.json && r.json.data && r.json.data.id;
  console.log('  orderB.id=' + cancelBId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo));

  out('  管理员登录（派单用）');
  r = await call('admin login', 'POST', '/api/auth/login', {
    username: 'admin', password: 'admin123',
    captcha: '0000', captchaKey: 'test-bypass', role: 'ADMIN'
  });
  state.adminToken = r.json && r.json.data && r.json.data.token;

  out('  陪诊师登录（接单用）');
  r = await call('escort login', 'POST', '/api/auth/login', {
    username: '13800002222', password: 'Test1234',
    captcha: '0000', captchaKey: 'test-bypass', role: 'ESCORT'
  });
  state.escortToken = r.json && r.json.data && r.json.data.token;
  state.escortUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  escort userId=' + state.escortUserId + ' token=' + shortToken(state.escortToken));

  r = await call('dispatch B', 'POST', '/api/order/' + cancelBId + '/dispatch?escortId=' + state.escortUserId, undefined, state.adminToken);
  r = await call('order B status after dispatch', 'GET', '/api/order/' + cancelBId, undefined, state.familyToken);
  const dispatchedRefund = null; // detail 尚未取消，无退款
  console.log('  orderB status=' + (r.json && r.json.data && r.json.data.status));

  // 先记录 DISPATCHED 取消的退款（单独建订单 C）
  out('  5b-对照：DISPATCHED 状态取消的退款比例');
  r = await call('create order for cancel C', 'POST', '/api/order', {
    requestId: 'T05-CC-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '儿科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelCId = r.json && r.json.data && r.json.data.id;
  r = await call('dispatch C', 'POST', '/api/order/' + cancelCId + '/dispatch?escortId=' + state.escortUserId, undefined, state.adminToken);
  r = await call('cancel C (DISPATCHED)', 'PUT', '/api/order/' + cancelCId + '/cancel', { cancelReason: 'T05测试-已派单取消' }, state.familyToken);
  r = await call('order C detail after cancel', 'GET', '/api/order/' + cancelCId, undefined, state.familyToken);
  console.log('  orderC status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount));

  r = await call('accept B', 'POST', '/api/order/' + cancelBId + '/accept', undefined, state.escortToken);
  r = await call('order B status after accept', 'GET', '/api/order/' + cancelBId, undefined, state.familyToken);
  console.log('  orderB status=' + (r.json && r.json.data && r.json.data.status));
  r = await call('cancel B (ACCEPTED/T10)', 'PUT', '/api/order/' + cancelBId + '/cancel', { cancelReason: 'T05测试-已接单取消' }, state.familyToken);
  const cancelBResp = r.text;
  r = await call('order B detail after cancel', 'GET', '/api/order/' + cancelBId, undefined, state.familyToken);
  console.log('  orderB status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount) +
              '  (cancel接口响应体=' + cancelBResp + ')');

  // ========== 必测6：通知列表 + 未读数 ==========
  out('必测6 通知列表 GET /api/notification/page?current=1&size=20');
  r = await call('notification page', 'GET', '/api/notification/page?current=1&size=20', undefined, state.familyToken, { full: true });
  const page = (r.json && r.json.data) || {};
  const records = page.records || [];
  const unread = records.filter(x => x.isRead === 0 || x.isRead === false).length;
  console.log('  total=' + page.total + ' records=' + records.length +
              ' 未读数(按records.isRead统计)=' + unread +
              '  （后端无独立未读数端点）');

  // ========== 必测18 前置：构造 mock 500，验证指数退避重试 ==========
  out('必测18 构造 mock 500（department=TRIGGER_500）-> 生成告知书，观察重试');
  r = await call('create order TRIGGER_500', 'POST', '/api/order', {
    requestId: 'T05-R500-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: 'TRIGGER_500',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const retryOrderId = r.json && r.json.data && r.json.data.id;
  console.log('  retryOrder.id=' + retryOrderId);
  const t0 = Date.now();
  r = await call('generate notice with 500', 'POST', '/api/ai/notice/generate', { orderId: retryOrderId }, state.familyToken, { full: true });
  const elapsed = Date.now() - t0;
  const retryContentId = r.json && r.json.data && r.json.data.id;
  console.log('  总耗时=' + elapsed + ' ms (4次尝试，理论退避1s+2s+4s=7000ms)');
  console.log('  data.id=' + retryContentId + ' content=' + String(r.json && r.json.data && r.json.data.content).slice(0, 120));

  console.log('\n===== 关键ID汇总（供SQL与后续脚本） =====');
  console.log(JSON.stringify({
    familyUserId: (await (async () => {
      const rr = await fetch(BASE + '/api/auth/info', { headers: { Authorization: 'Bearer ' + state.familyToken } });
      const jj = await rr.json(); return jj.data && jj.data.id;
    })()),
    elderlyId: state.elderlyId,
    orderId: state.orderId,
    orderNo: state.orderNo,
    noticeContentId: state.noticeId,
    cancelAId, cancelBId, cancelCId,
    retryOrderId, retryContentId
  }, null, 2));
}

main().catch(e => { console.error('实测脚本异常: ' + (e && e.stack || e)); process.exit(1); });