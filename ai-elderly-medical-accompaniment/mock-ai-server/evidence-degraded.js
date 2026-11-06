// T-05 实测脚本（mock 停止态）：必测17 降级链路 + 熔断阈值生效
// 用法（先停掉 mock-ai-server/server.js，后端 8080 保持运行）：
//   node mock-ai-server/evidence-degraded.js <orderId>
// 说明：连续调用 4 次；结合此前 500(1次) 与 400(1次) 失败，第3次即累计第5次失败触发熔断，
//       第4次应直接降级（耗时接近0，且不再产生 FAILED 任务行）。

const BASE = 'http://127.0.0.1:8080';
const ORDER_ID = Number(process.argv[2]);

function ts() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}
async function call(label, method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
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
if (!ORDER_ID) { console.error('用法: node mock-ai-server/evidence-degraded.js <orderId>'); process.exit(1); }

(async () => {
  console.log('===== mock 停止态：先确认 9002 不可连接 =====');
  try {
    await fetch('http://127.0.0.1:9002/llm/chat', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"prompt":"ping"}' });
    console.log('⚠ 9002 仍可连接，请先停止 mock 再运行本脚本');
  } catch (e) {
    console.log('9002 连接失败: ' + e.message + '  (符合 mock 停止态预期)');
  }

  console.log('\n===== 必测1（复登） =====');
  let r = await call('login', 'POST', '/api/auth/login', {
    username: '13800001111', password: 'Test1234', captcha: '0000', captchaKey: 'test-bypass', role: 'FAMILY'
  });
  const token = r.json && r.json.data && r.json.data.token;
  console.log('  role=' + (r.json && r.json.data && r.json.data.role) + ' token=' + (token ? token.slice(0, 24) + '...' : token));

  let firstContentId = null;
  for (let i = 1; i <= 4; i++) {
    console.log(`\n===== 必测17 第${i}次 POST /api/ai/notice/generate（mock 停止态） =====`);
    r = await call('notice generate #' + i, 'POST', '/api/ai/notice/generate', { orderId: ORDER_ID }, token);
    const data = (r.json && r.json.data) || {};
    console.log(`  HTTP=${r.status} code=${r.json && r.json.code} data.id=${data.id} (${typeof data.id})` +
                ` contentType=${data.contentType} content=${JSON.stringify(String(data.content || '').slice(0, 160))}`);
    if (i === 1 && data.id) {
      firstContentId = data.id;
      console.log('\n===== 必测17 GET /api/ai/content/{降级id} =====');
      const g = await call('content get', 'GET', '/api/ai/content/' + data.id, undefined, token);
      const cd = (g.json && g.json.data) || {};
      console.log('  HTTP=' + g.status + ' id=' + cd.id + ' contentType=' + cd.contentType +
                  ' disclaimer=' + cd.disclaimer + ' content=' + JSON.stringify(String(cd.content || '')));
    }
  }
  console.log('\n===== 第4次说明 =====');
  console.log('若第3次后熔断器打开（日志出现“熔断器[llm]打开, 连续失败5次”），第4次耗时应在毫秒级且不产生 FAILED 行。');
  console.log('firstContentId=' + firstContentId);
})().catch(e => { console.error('脚本异常: ' + (e && e.stack || e)); process.exit(1); });