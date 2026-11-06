// T-05 实测脚本（mock 运行态）：必测 1-6 + 必测 16 的 SUCCESS 内容摘要
// 用法（后端 8080、mock 9001/9002、MySQL 均已启动）：
//   node mock-ai-server/evidence-t05-family.js
// 说明：仅使用 Node 内置能力；不修改任何业务代码与配置。
// 取证纪律：所有中文请求体均由 Node 以 UTF-8 编码发送，不使用 Git Bash 直传中文。
// 注：本文件为 T-05 新增取证脚本；不改动既有 evidence-family.js。

const BASE = 'http://127.0.0.1:8080';
const crypto = require('crypto');

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
function out(label) { console.log(`\n===== ${label} =====`); }
function sha16(s) { return crypto.createHash('sha256').update(s, 'utf8').digest('hex').slice(0, 16); }

const state = {};

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
  const limit = opts.full ? 100000 : 3000;
  console.log('  body: ' + (text.length > limit ? text.slice(0, limit) + '...[truncated,total=' + text.length + ']' : text));
  return { status: res.status, text, json, ms, t0 };
}

async function login(username, password, role) {
  return call('POST', '/api/auth/login', {
    username, password, captcha: '0000', captchaKey: 'test-bypass', role
  });
}

async function main() {
  out('必测1 家属登录（工单写 /api/user/login，实际映射 /api/auth/login，两条均实测）');
  let r = await call('POST', '/api/user/login', {
    username: '13800001111', password: 'Test1234',
    captcha: '0000', captchaKey: 'test-bypass', role: 'FAMILY'
  });
  console.log('  说明：/api/user/login 实测状态码=' + r.status + '（代码实际映射见 AuthController @RequestMapping("/api/auth")）');

  r = await login('13800001111', 'Test1234', 'FAMILY');
  state.familyToken = r.json && r.json.data && r.json.data.token;
  state.familyUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) +
              '  data.role=' + (r.json && r.json.data && r.json.data.role) +
              '  data.userId=' + state.familyUserId +
              '  token(first24)=' + shortToken(state.familyToken));
  if (!state.familyToken) throw new Error('家属登录失败，停止后续步骤');

  out('必测2a 老人档案列表 GET /api/elderly/page?current=1&size=10');
  r = await call('GET', '/api/elderly/page?current=1&size=10', undefined, state.familyToken);
  const list = (r.json && r.json.data && r.json.data.records) || [];
  console.log('  total=' + (r.json && r.json.data && r.json.data.total) +
              '  first.id=' + (list[0] && list[0].id) +
              '  first.name=' + (list[0] && list[0].name));
  state.elderlyId = list[0] && list[0].id;

  out('必测2b 新增老人档案 POST /api/elderly');
  const elderName = 'T05实测老人' + Date.now().toString().slice(-6);
  const elderBody = {
    name: elderName, gender: 'MALE', birthDate: '1952-06-18',
    idCard: '330102195206181234', phone: '13900002222',
    address: '广州市测试路T05号', emergencyContact: '13800001111',
    emergencyName: '测试家属', mobility: 'ASSIST',
    allergyHistory: '青霉素过敏', chronicDisease: '高血压', remark: 'T05实测新增'
  };
  r = await call('POST', '/api/elderly', elderBody, state.familyToken);
  console.log('  create response code=' + (r.json && r.json.code) + ' data=' + JSON.stringify(r.json && r.json.data));

  out('必测2c 新增后回查 GET /api/elderly/page?current=1&size=20');
  r = await call('GET', '/api/elderly/page?current=1&size=20', undefined, state.familyToken);
  const newList = (r.json && r.json.data && r.json.data.records) || [];
  const created = newList.find(x => x.name === elderName);
  state.elderlyId = created ? created.id : state.elderlyId;
  console.log('  新增回查 name=' + elderName + ' 真实id=' + (created && created.id));

  out('必测3a 费用试算 POST /api/order/estimate {serviceType:BASIC,duration:2}');
  r = await call('POST', '/api/order/estimate', { serviceType: 'BASIC', duration: 2 }, state.familyToken);
  const amount = r.json && r.json.data && r.json.data.totalAmount;
  console.log('  totalAmount=' + amount + ' unitPrice=' + (r.json && r.json.data && r.json.data.unitPrice));

  out('必测3b 下单 POST /api/order（appointmentTime 空格格式 yyyy-MM-dd HH:mm:ss）');
  const appt = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  const orderBody = {
    requestId: 'T05-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '心内科',
    serviceType: 'BASIC', appointmentTime: appt, duration: 2, amount: amount
  };
  r = await call('POST', '/api/order', orderBody, state.familyToken);
  state.orderId = r.json && r.json.data && r.json.data.id;
  state.orderNo = r.json && r.json.data && r.json.data.orderNo;
  console.log('  order.id=' + state.orderId + '  order.orderNo=' + state.orderNo +
              '  amount=' + (r.json && r.json.data && r.json.data.amount) +
              '  appointmentTime=' + (r.json && r.json.data && r.json.data.appointmentTime));

  out('必测3c 订单列表 GET /api/order/page?current=1&size=10');
  r = await call('GET', '/api/order/page?current=1&size=10', undefined, state.familyToken);
  const orders = (r.json && r.json.data && r.json.data.records) || [];
  const found = orders.find(x => x.id === state.orderId);
  console.log('  列表中命中订单: id=' + (found && found.id) + ' orderNo=' + (found && found.orderNo) +
              ' status=' + (found && found.status) + ' amount=' + (found && found.amount));

  out('必测4a 生成告知书 POST /api/ai/notice/generate（mock 运行态）');
  r = await call('POST', '/api/ai/notice/generate', { orderId: state.orderId }, state.familyToken, { full: true });
  state.noticeId = r.json && r.json.data && r.json.data.id;
  const noticeContent = (r.json && r.json.data && r.json.data.content) || '';
  console.log('  data.id=' + state.noticeId + ' (type=' + typeof state.noticeId + ')');
  console.log('  data.contentType=' + (r.json && r.json.data && r.json.data.contentType));
  console.log('  data.content len=' + noticeContent.length +
              '  contains 降级文案="AI服务暂不可用" ? ' + noticeContent.includes('AI服务暂不可用') +
              '  contains 字符串null ? ' + noticeContent.includes('null'));
  // SC-02：预约时间回填可能为空格格式（yyyy-MM-dd HH:mm:ss）或 ISO T 格式（yyyy-MM-ddTHH:mm:ss），两种都接受
  const apptIso = appt.replace(' ', 'T');
  console.log('  回填校验: 科室=' + noticeContent.includes('心内科') +
              ' 预约时间=' + (noticeContent.includes(appt) || noticeContent.includes(apptIso)) +
              ' 行动能力=' + noticeContent.includes('ASSIST') +
              ' 过敏史=' + noticeContent.includes('青霉素过敏'));
  console.log('  data.content 前400字符: ' + noticeContent.slice(0, 400));
  console.log('  sha256(data.content)前16位=' + sha16(noticeContent));

  out('必测4b 查看告知书 GET /api/ai/content/{id}');
  r = await call('GET', '/api/ai/content/' + state.noticeId, undefined, state.familyToken, { full: true });
  const got = (r.json && r.json.data) || {};
  console.log('  id=' + got.id + ' contentType=' + got.contentType +
              ' 内容一致=' + (got.content === noticeContent) +
              ' disclaimer=' + got.disclaimer);

  out('必测5a 新建订单 A -> PENDING_DISPATCH 取消');
  const appt2 = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  r = await call('POST', '/api/order', {
    requestId: 'T05-CA-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '内科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelAId = r.json && r.json.data && r.json.data.id;
  console.log('  orderA.id=' + cancelAId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo));
  r = await call('PUT', '/api/order/' + cancelAId + '/cancel', { cancelReason: 'T05测试-待派单取消' }, state.familyToken);
  const cancelAResp = r.text;
  r = await call('GET', '/api/order/' + cancelAId, undefined, state.familyToken);
  console.log('  orderA status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount) +
              '  (cancel接口响应体=' + cancelAResp + ')');

  out('必测5b 管理员/陪诊师登录（派单+接单用）');
  r = await login('admin', 'admin123', 'ADMIN');
  state.adminToken = r.json && r.json.data && r.json.data.token;
  console.log('  admin code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' token=' + shortToken(state.adminToken));
  r = await login('13800002222', 'Test1234', 'ESCORT');
  state.escortToken = r.json && r.json.data && r.json.data.token;
  state.escortUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  escort code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' userId=' + state.escortUserId + ' token=' + shortToken(state.escortToken));

  out('必测5c 新建订单 C -> 派单 -> DISPATCHED 取消');
  r = await call('POST', '/api/order', {
    requestId: 'T05-CC-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '儿科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelCId = r.json && r.json.data && r.json.data.id;
  console.log('  orderC.id=' + cancelCId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo));
  r = await call('POST', '/api/order/' + cancelCId + '/dispatch?escortId=' + state.escortUserId, undefined, state.adminToken);
  r = await call('GET', '/api/order/' + cancelCId, undefined, state.familyToken);
  console.log('  orderC 派单后 status=' + (r.json && r.json.data && r.json.data.status) +
              ' escortId=' + (r.json && r.json.data && r.json.data.escortId));
  r = await call('PUT', '/api/order/' + cancelCId + '/cancel', { cancelReason: 'T05测试-已派单取消' }, state.familyToken);
  const cancelCResp = r.text;
  r = await call('GET', '/api/order/' + cancelCId, undefined, state.familyToken);
  console.log('  orderC status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount) +
              '  (cancel接口响应体=' + cancelCResp + ')');

  out('必测5d 新建订单 B -> 派单 -> 接单(ACCEPTED/T10) -> 取消');
  r = await call('POST', '/api/order', {
    requestId: 'T05-CB-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId: state.elderlyId, hospitalId: 1, department: '外科',
    serviceType: 'BASIC', appointmentTime: appt2, duration: 2, amount: amount
  }, state.familyToken);
  const cancelBId = r.json && r.json.data && r.json.data.id;
  console.log('  orderB.id=' + cancelBId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo));
  r = await call('POST', '/api/order/' + cancelBId + '/dispatch?escortId=' + state.escortUserId, undefined, state.adminToken);
  r = await call('POST', '/api/order/' + cancelBId + '/accept', undefined, state.escortToken);
  r = await call('GET', '/api/order/' + cancelBId, undefined, state.familyToken);
  console.log('  orderB 接单后 status=' + (r.json && r.json.data && r.json.data.status));
  r = await call('PUT', '/api/order/' + cancelBId + '/cancel', { cancelReason: 'T05测试-已接单取消' }, state.familyToken);
  const cancelBResp = r.text;
  r = await call('GET', '/api/order/' + cancelBId, undefined, state.familyToken);
  console.log('  orderB status=' + (r.json && r.json.data && r.json.data.status) +
              ' payStatus=' + (r.json && r.json.data && r.json.data.payStatus) +
              ' amount=' + (r.json && r.json.data && r.json.data.amount) +
              ' refundAmount=' + (r.json && r.json.data && r.json.data.refundAmount) +
              '  (cancel接口响应体=' + cancelBResp + ')');

  out('必测6 通知列表 GET /api/notification/page?current=1&size=20');
  r = await call('GET', '/api/notification/page?current=1&size=20', undefined, state.familyToken, { full: true });
  const page = (r.json && r.json.data) || {};
  const records = page.records || [];
  const unread = records.filter(x => x.isRead === 0 || x.isRead === false).length;
  console.log('  total=' + page.total + ' records=' + records.length +
              ' 未读数(按本次返回 records.isRead 统计)=' + unread +
              '  （后端未提供独立未读数端点，见 NotifyController 仅 page/read/read-all）');

  console.log('\n===== 关键ID汇总（供 SQL 与后续脚本） =====');
  console.log(JSON.stringify({
    familyUserId: state.familyUserId,
    elderlyId: state.elderlyId,
    orderId: state.orderId,
    orderNo: state.orderNo,
    noticeContentId: state.noticeId,
    noticeContentSha256_16: sha16(noticeContent),
    cancelAId, cancelBId, cancelCId
  }, null, 2));
}
main().catch(e => { console.error('实测脚本异常: ' + (e && e.stack || e)); process.exit(1); });