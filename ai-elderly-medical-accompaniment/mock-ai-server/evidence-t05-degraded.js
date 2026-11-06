// T-05 实测脚本（mock 停止态）：必测17 降级链路 + 熔断阈值(5)行为
// 用法（先停止 mock-ai-server/server.js，后端 8080 保持运行）：
//   node mock-ai-server/evidence-t05-degraded.js <orderId>
// 说明：连续调用 6 次。第1~5次：真实连接失败 -> ai_task FAILED，熔断计数 1~5；
//       第5次失败后熔断器打开；第6次：allowRequest=false，直接走 degraded()，不再产生 FAILED 行。
// 注：本文件为 T-05 新增取证脚本；不改动既有 evidence-degraded.js。

const BASE = 'http://127.0.0.1:8080';
const ORDER_ID = Number(process.argv[2]);
const crypto = require('crypto');

function ts() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}
function sha16(s) { return crypto.createHash('sha256').update(s, 'utf8').digest('hex').slice(0, 16); }

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
if (!ORDER_ID) { console.error('用法: node mock-ai-server/evidence-t05-degraded.js <orderId>'); process.exit(1); }

(async () => {
  console.log('===== mock 停止态：先确认 9002 不可连接 =====');
  try {
    await fetch('http://127.0.0.1:9002/llm/chat', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"prompt":"ping"}' });
    console.log('⚠ 9002 仍可连接，请先停止 mock 再运行本脚本');
  } catch (e) {
    console.log('9002 连接失败: ' + e.message + '  (符合 mock 停止态预期)');
  }

  console.log('\n===== 必测1（复登） =====');
  let r = await call('POST', '/api/auth/login', {
    username: '13800001111', password: 'Test1234', captcha: '0000', captchaKey: 'test-bypass', role: 'FAMILY'
  });
  const token = r.json && r.json.data && r.json.data.token;
  console.log('  role=' + (r.json && r.json.data && r.json.data.role) + ' token=' + (token ? token.slice(0, 24) + '...' : token));
  if (!token) throw new Error('家属登录失败，停止后续步骤');

  let firstContentId = null;
  const ids = [];
  const times = [];
  for (let i = 1; i <= 6; i++) {
    console.log(`\n===== 必测17 第${i}次 POST /api/ai/notice/generate（mock 停止态） =====`);
    r = await call('POST', '/api/ai/notice/generate', { orderId: ORDER_ID }, token);
    const data = (r.json && r.json.data) || {};
    times.push(r.ms);
    ids.push(data.id);
    const content = String(data.content || '');
    console.log(`  HTTP=${r.status} code=${r.json && r.json.code} data.id=${data.id} (${typeof data.id})` +
                ` contentType=${data.contentType}`);
    console.log(`  降级文案命中=${content.includes('AI服务暂时不可用')}` +
                ` 字符串null=${content.includes('null')}` +
                ` sha256(content)前16位=${sha16(content)}`);
    console.log(`  content=${JSON.stringify(content)}`);
    if (i === 1 && data.id) {
      firstContentId = data.id;
      console.log('\n===== 必测17 GET /api/ai/content/{降级id} =====');
      const g = await call('GET', '/api/ai/content/' + data.id, undefined, token);
      const cd = (g.json && g.json.data) || {};
      console.log('  HTTP=' + g.status + ' id=' + cd.id + ' contentType=' + cd.contentType +
                  ' disclaimer=' + cd.disclaimer + ' content可渲染(非空)=' + Boolean(cd.content));
    }
  }
  console.log('\n===== 6 次调用汇总 =====');
  console.log('  contentIds=' + JSON.stringify(ids));
  console.log('  各次耗时(ms)=' + JSON.stringify(times));
  console.log('  第1~5次应为 FAILED+降级；第6次熔断打开，应无 FAILED 行，仅 DEGRADED。');
  console.log('firstContentId=' + firstContentId);
})().catch(e => { console.error('脚本异常: ' + (e && e.stack || e)); process.exit(1); });