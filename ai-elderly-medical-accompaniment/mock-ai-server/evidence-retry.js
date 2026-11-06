// T-05 实测脚本：必测18 重试策略
// 用法：
//   node mock-ai-server/evidence-retry.js 500   # mock 运行态，server.js 对 TRIGGER_500 返回 500
//   node mock-ai-server/evidence-retry.js 400   # mock 停止态，retry-fixture.js 9002 400 返回固定 400
// 说明：
//   500 模式：应重试 3 次（退避 1s/2s/4s，共 4 次请求），ai_task.retry_count=3，总耗时约 7s。
//   400 模式：非 5xx/429/超时，应直接失败不重试，ai_task.retry_count=0，总耗时 < 1s。
//   仅使用 Node 内置能力，不修改任何业务代码与配置。

const BASE = 'http://127.0.0.1:8080';
const MODE = process.argv[2] || '500';

function ts() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}
function fmtLocal(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
async function call(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json; charset=utf-8' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const t0 = Date.now();
  const stamp = ts();
  const res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await res.text();
  console.log(`[${stamp}] ${method} ${path}`);
  if (body !== undefined) console.log('  request body: ' + JSON.stringify(body));
  console.log(`  -> HTTP ${res.status}  (${Date.now() - t0} ms)`);
  console.log('  body: ' + text);
  let json = null; try { json = JSON.parse(text); } catch (e) {}
  return { status: res.status, text, json, ms: Date.now() - t0 };
}

(async () => {
  if (MODE !== '500' && MODE !== '400') {
    console.error('用法: node mock-ai-server/evidence-retry.js 500|400'); process.exit(1);
  }
  console.log('===== 必测18 模式=' + MODE + ' =====');
  let r = await call('POST', '/api/auth/login', {
    username: '13800001111', password: 'Test1234', captcha: '0000', captchaKey: 'test-bypass', role: 'FAMILY'
  });
  const token = r.json && r.json.data && r.json.data.token;
  if (!token) throw new Error('家属登录失败，停止后续步骤');
  console.log('  token=' + token.slice(0, 24) + '...');

  r = await call('GET', '/api/elderly/page?current=1&size=10', undefined, token);
  const elderlyId = r.json && r.json.data && r.json.data.records && r.json.data.records[0] && r.json.data.records[0].id;
  console.log('  使用老人档案 id=' + elderlyId);

  r = await call('POST', '/api/order/estimate', { serviceType: 'BASIC', duration: 2 }, token);
  const amount = r.json && r.json.data && r.json.data.totalAmount;

  const department = MODE === '500' ? 'TRIGGER_500' : 'TRIGGER_400';
  const appt = fmtLocal(new Date(Date.now() + 24 * 3600 * 1000));
  r = await call('POST', '/api/order', {
    requestId: 'T05-R' + MODE + '-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId, hospitalId: 1, department,
    serviceType: 'BASIC', appointmentTime: appt, duration: 2, amount
  }, token);
  const orderId = r.json && r.json.data && r.json.data.id;
  console.log('  构造订单 id=' + orderId + ' orderNo=' + (r.json && r.json.data && r.json.data.orderNo) +
              ' department=' + department);

  console.log('\n===== 发起告知书生成（观察重试耗时） =====');
  const t0 = Date.now();
  r = await call('POST', '/api/ai/notice/generate', { orderId }, token);
  const elapsed = Date.now() - t0;
  const data = (r.json && r.json.data) || {};
  console.log('  总耗时=' + elapsed + ' ms');
  console.log('  data.id=' + data.id + ' (' + typeof data.id + ') contentType=' + data.contentType +
              ' 降级文案=' + String(data.content || '').includes('AI服务暂时不可用'));
  if (MODE === '500') {
    console.log('  判定参考：4 次请求理论间隔 1s+2s+4s，总耗时约 7000ms；ai_task.retry_count 应为 3。');
  } else {
    console.log('  判定参考：非 5xx/429/超时，应只请求 1 次；总耗时 < 1000ms；ai_task.retry_count 应为 0。');
  }
  console.log('  后续请用 SQL 查询该订单最新 NOTICE 任务的 retry_count / status / cost_ms。orderId=' + orderId);
})().catch(e => { console.error('脚本异常: ' + (e && e.stack || e)); process.exit(1); });